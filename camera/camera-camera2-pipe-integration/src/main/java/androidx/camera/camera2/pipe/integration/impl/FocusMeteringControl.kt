/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.camera.camera2.pipe.integration.impl

import android.graphics.PointF
import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.params.MeteringRectangle
import android.util.Rational
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.AeMode
import androidx.camera.camera2.pipe.Result3A
import androidx.camera.camera2.pipe.integration.adapter.asListenableFuture
import androidx.camera.camera2.pipe.integration.config.CameraScope
import androidx.camera.core.CameraControl
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.FocusMeteringResult
import androidx.camera.core.MeteringPoint
import androidx.camera.core.Preview
import androidx.camera.core.impl.CameraControlInternal
import com.google.common.util.concurrent.ListenableFuture
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import javax.inject.Inject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.launch

/**
 * Implementation of focus and metering controls exposed by [CameraControlInternal].
 */
@CameraScope
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
class FocusMeteringControl @Inject constructor(
    val cameraProperties: CameraProperties,
    val threads: UseCaseThreads,
) : UseCaseCameraControl {
    private var _useCaseCamera: UseCaseCamera? = null
    override var useCaseCamera: UseCaseCamera?
        get() = _useCaseCamera
        set(value) {
            _useCaseCamera = value

            // reset to null since preview ratio may not be applicable for current runningUseCases
            previewAspectRatio = null
            _useCaseCamera?.runningUseCasesLiveData?.observeForever { useCases ->
                useCases.forEach { useCase ->
                    if (useCase is Preview) {
                        useCase.attachedSurfaceResolution?.apply {
                            previewAspectRatio = Rational(width, height)
                        }
                    }
                }
            }
        }

    override fun reset() {
        cancelFocusAndMeteringAsync()
    }

    private var previewAspectRatio: Rational? = null
    private val sensorRect by lazy {
        // TODO("b/262225455"): use the actual crop sensor region like in camera-camera2
        cameraProperties.metadata[CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE]!!
    }

    private val defaultAspectRatio: Rational
        get() = previewAspectRatio ?: Rational(sensorRect.width(), sensorRect.height())

    private val maxAfRegionCount =
        cameraProperties.metadata.getOrDefault(CameraCharacteristics.CONTROL_MAX_REGIONS_AF, 0)
    private val maxAeRegionCount =
        cameraProperties.metadata.getOrDefault(CameraCharacteristics.CONTROL_MAX_REGIONS_AE, 0)
    private val maxAwbRegionCount =
        cameraProperties.metadata.getOrDefault(CameraCharacteristics.CONTROL_MAX_REGIONS_AWB, 0)

    fun startFocusAndMetering(
        action: FocusMeteringAction
    ): ListenableFuture<FocusMeteringResult> {
        val signal = CompletableDeferred<FocusMeteringResult>()

        useCaseCamera?.let { useCaseCamera ->
            threads.sequentialScope.launch {
                signal.complete(
                    useCaseCamera.requestControl.startFocusAndMeteringAsync(
                        aeRegions = meteringRegionsFromMeteringPoints(
                            action.meteringPointsAe,
                            maxAeRegionCount,
                            sensorRect,
                            defaultAspectRatio
                        ),
                        afRegions = meteringRegionsFromMeteringPoints(
                            action.meteringPointsAf,
                            maxAfRegionCount,
                            sensorRect,
                            defaultAspectRatio
                        ),
                        awbRegions = meteringRegionsFromMeteringPoints(
                            action.meteringPointsAwb,
                            maxAwbRegionCount,
                            sensorRect,
                            defaultAspectRatio
                        ),
                        afTriggerStartAeMode = cameraProperties.getSupportedAeMode(AeMode.ON)
                    ).await().toFocusMeteringResult(true)
                )
            }
        } ?: run {
            signal.completeExceptionally(
                CameraControl.OperationCanceledException("Camera is not active.")
            )
        }

        return signal.asListenableFuture()
    }

    fun isFocusMeteringSupported(
        action: FocusMeteringAction
    ): Boolean {
        val rectanglesAe = meteringRegionsFromMeteringPoints(
            action.meteringPointsAe,
            maxAeRegionCount,
            sensorRect,
            defaultAspectRatio
        )
        val rectanglesAf = meteringRegionsFromMeteringPoints(
            action.meteringPointsAf,
            maxAfRegionCount,
            sensorRect,
            defaultAspectRatio
        )
        val rectanglesAwb = meteringRegionsFromMeteringPoints(
            action.meteringPointsAwb,
            maxAwbRegionCount,
            sensorRect,
            defaultAspectRatio
        )
        return rectanglesAe.isNotEmpty() || rectanglesAf.isNotEmpty() || rectanglesAwb.isNotEmpty()
    }

    // TODO: Move this to a lower level so it is automatically checked for all requests
    private fun CameraProperties.getSupportedAeMode(preferredMode: AeMode): AeMode {
        val modes = metadata[CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES]?.map {
            AeMode.fromIntOrNull(it)
        } ?: return AeMode.OFF

        // if preferredMode is supported, use it
        if (modes.contains(preferredMode)) {
            return preferredMode
        }

        // if not found, priority is AE_ON > AE_OFF
        return if (modes.contains(AeMode.ON)) {
            AeMode.ON
        } else AeMode.OFF
    }

    fun cancelFocusAndMeteringAsync(): Deferred<Unit> {
        return CompletableDeferred(null)
    }

    /**
     * Give whether auto focus trigger was desired, this method transforms a [Result3A] into
     * [FocusMeteringResult] by checking if the auto focus was locked in a focused state.
     */
    private fun Result3A.toFocusMeteringResult(shouldTriggerAf: Boolean): FocusMeteringResult {
        if (this.status != Result3A.Status.OK) {
            return FocusMeteringResult.create(false)
        }
        val isFocusSuccessful =
            if (shouldTriggerAf)
                this.frameMetadata?.get(CaptureResult.CONTROL_AF_STATE) ==
                    CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED
            else true

        return FocusMeteringResult.create(isFocusSuccessful)
    }

    companion object {
        const val METERING_WEIGHT_DEFAULT = MeteringRectangle.METERING_WEIGHT_MAX

        fun meteringRegionsFromMeteringPoints(
            meteringPoints: List<MeteringPoint>,
            maxRegionCount: Int,
            cropSensorRegion: Rect,
            defaultAspectRatio: Rational,
        ): List<MeteringRectangle> {
            if (meteringPoints.isEmpty() || maxRegionCount == 0) {
                return emptyList()
            }
            val meteringRegions: MutableList<MeteringRectangle> = ArrayList()
            val cropRegionAspectRatio =
                Rational(cropSensorRegion.width(), cropSensorRegion.height())

            // TODO(sushilnath@): limit the number of metering regions to what is supported by the
            // device.
            for (meteringPoint in meteringPoints) {
                // Only enable at most maxRegionCount.
                if (meteringRegions.size >= maxRegionCount) {
                    break
                }
                if (!isValid(meteringPoint)) {
                    continue
                }
                // TODO(sushilnath@): Use the zoom based crop region aspect ratio instead of sensor
                // active array aspect ratio.
                val adjustedPoint: PointF =
                    getFovAdjustedPoint(meteringPoint, cropRegionAspectRatio, defaultAspectRatio)
                val meteringRectangle: MeteringRectangle =
                    getMeteringRect(adjustedPoint, meteringPoint.size, cropSensorRegion)
                meteringRegions.add(meteringRectangle)
            }
            return meteringRegions
        }

        // Illustration ref : https://source.android.com/devices/camera/camera3_crop_reprocess
        private fun getFovAdjustedPoint(
            meteringPoint: MeteringPoint,
            cropRegionAspectRatio: Rational,
            defaultAspectRatio: Rational
        ): PointF {
            // Use default aspect ratio unless there is a custom aspect ratio in MeteringPoint.
            val fovAspectRatio = meteringPoint.surfaceAspectRatio ?: defaultAspectRatio
            if (fovAspectRatio != cropRegionAspectRatio) {
                if (fovAspectRatio.compareTo(cropRegionAspectRatio) > 0) {
                    val adjustedPoint = PointF(meteringPoint.x, meteringPoint.y)
                    // The crop region is taller than the FOV, top and bottom of the crop region is
                    // cropped.
                    val verticalPadding =
                        (fovAspectRatio.toDouble() / cropRegionAspectRatio.toDouble()).toFloat()
                    val topPadding = ((verticalPadding - 1.0) / 2).toFloat()
                    adjustedPoint.y = (topPadding + adjustedPoint.y) * (1f / verticalPadding)
                    return adjustedPoint
                } else {
                    val adjustedPoint = PointF(meteringPoint.x, meteringPoint.y)
                    // The crop region is wider than the FOV, left and right side of crop region is
                    // cropped
                    val horizontalPadding =
                        (cropRegionAspectRatio.toDouble() / fovAspectRatio.toDouble()).toFloat()
                    val leftPadding = ((horizontalPadding - 1.0) / 2).toFloat()
                    adjustedPoint.x = (leftPadding + adjustedPoint.x) * (1f / horizontalPadding)
                    return adjustedPoint
                }
            }
            return PointF(meteringPoint.x, meteringPoint.y)
        }

        // Given a normalized PointF and normalized size factor for width and height, calculate
        // the corresponding metering rectangle for the given crop region. The necessary
        // constraints are applies to make sure that the metering rectangle is bonded within the
        // crop region.
        private fun getMeteringRect(
            normalizedPointF: PointF,
            normalizedSize: Float,
            cropRegion: Rect
        ): MeteringRectangle {
            val centerX = (cropRegion.left + normalizedPointF.x * cropRegion.width()).toInt()
            val centerY = (cropRegion.top + normalizedPointF.y * cropRegion.height()).toInt()
            val width = (normalizedSize * cropRegion.width()).toInt()
            val height = (normalizedSize * cropRegion.height()).toInt()
            val focusRect = Rect(
                centerX - width / 2,
                centerY - height / 2,
                centerX + width / 2,
                centerY + height / 2
            )
            focusRect.left = focusRect.left.coerceIn(cropRegion.left, cropRegion.right)
            focusRect.right = focusRect.right.coerceIn(cropRegion.left, cropRegion.right)
            focusRect.top = focusRect.top.coerceIn(cropRegion.top, cropRegion.bottom)
            focusRect.bottom = focusRect.bottom.coerceIn(cropRegion.top, cropRegion.bottom)
            return MeteringRectangle(focusRect, METERING_WEIGHT_DEFAULT)
        }

        private fun isValid(pt: MeteringPoint): Boolean {
            return pt.x >= 0f && pt.x <= 1f && pt.y >= 0f && pt.y <= 1f
        }
    }

    @Module
    abstract class Bindings {
        @Binds
        @IntoSet
        abstract fun provideControls(
            focusMeteringControl: FocusMeteringControl
        ): UseCaseCameraControl
    }
}
