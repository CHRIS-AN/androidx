/*
 * Copyright (C) 2021 The Android Open Source Project
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

package androidx.health.services.client.impl.request

import android.os.Parcelable
import androidx.annotation.RestrictTo
import androidx.health.services.client.data.ProtoParcelable
import androidx.health.services.client.proto.RequestsProto

/**
 * Request for enabling/disabling auto pause/resume.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class AutoPauseAndResumeConfigRequest(
    public val packageName: String,
    public val shouldEnable: Boolean,
) : ProtoParcelable<RequestsProto.AutoPauseAndResumeConfigRequest>() {

    override val proto: RequestsProto.AutoPauseAndResumeConfigRequest =
        RequestsProto.AutoPauseAndResumeConfigRequest.newBuilder()
            .setPackageName(packageName)
            .setShouldEnable(shouldEnable)
            .build()

    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<AutoPauseAndResumeConfigRequest> = newCreator {
            val request = RequestsProto.AutoPauseAndResumeConfigRequest.parseFrom(it)
            AutoPauseAndResumeConfigRequest(request.packageName, request.shouldEnable)
        }
    }
}
