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

package androidx.window.embedding

import android.content.Intent
import android.util.LayoutDirection.LOCALE
import androidx.annotation.FloatRange
import androidx.annotation.IntDef
import androidx.annotation.IntRange
import androidx.core.util.Preconditions.checkArgument
import androidx.core.util.Preconditions.checkArgumentNonnegative

/**
 * Configuration rules for split placeholders.
 *
 * A placeholder activity is usually a mostly empty activity that temporarily occupies the secondary
 * container of a split. The placeholder is intended to be replaced when another activity with
 * content is launched in a dedicated [SplitPairRule]. The placeholder activity is then occluded by
 * the newly launched activity. The placeholder can provide some optional features but must not host
 * important UI elements exclusively, since the placeholder is not shown on some devices and screen
 * configurations, such as devices with small screens.
 *
 * Configuration rules can be added using [RuleController.addRule] or [RuleController.setRules].
 *
 * See
 * [Activity embedding](https://developer.android.com/guide/topics/large-screens/activity-embedding#placeholders)
 * for more information.
 */
class SplitPlaceholderRule : SplitRule {

    /**
     * Filters used to choose when to apply this rule. The rule may be used if any one of the
     * provided filters matches.
     */
    val filters: Set<ActivityFilter>

    /**
     * Intent to launch the placeholder activity.
     */
    val placeholderIntent: Intent

    /**
     * Determines whether the placeholder will show on top in a smaller window size after it first
     * appeared in a split with sufficient minimum width.
     */
    val isSticky: Boolean

    /**
     * Defines whether a container should be finished together when the associated placeholder
     * activity is being finished based on current presentation mode.
     */
    @Target(AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(FINISH_ALWAYS, FINISH_ADJACENT)
    internal annotation class SplitPlaceholderFinishBehavior

    /**
     * Determines what happens with the primary container when all activities are finished in the
     * associated placeholder container.
     * @see SplitPlaceholderFinishBehavior
     */
    @SplitPlaceholderFinishBehavior
    val finishPrimaryWithPlaceholder: Int

    internal constructor(
        filters: Set<ActivityFilter>,
        placeholderIntent: Intent,
        isSticky: Boolean,
        @SplitPlaceholderFinishBehavior finishPrimaryWithPlaceholder: Int = FINISH_ALWAYS,
        @IntRange(from = 0) minWidthDp: Int = DEFAULT_SPLIT_MIN_DIMENSION_DP,
        @IntRange(from = 0) minSmallestWidthDp: Int = DEFAULT_SPLIT_MIN_DIMENSION_DP,
        @FloatRange(from = 0.0, to = 1.0) splitRatio: Float = 0.5f,
        @LayoutDirection layoutDirection: Int = LOCALE
    ) : super(minWidthDp, minSmallestWidthDp, splitRatio, layoutDirection) {
        checkArgumentNonnegative(minWidthDp, "minWidthDp must be non-negative")
        checkArgumentNonnegative(minSmallestWidthDp, "minSmallestWidthDp must be non-negative")
        checkArgument(splitRatio in 0.0..1.0, "splitRatio must be in 0.0..1.0 range")
        checkArgument(finishPrimaryWithPlaceholder != FINISH_NEVER,
            "FINISH_NEVER is not a valid configuration for SplitPlaceholderRule. " +
                "Please use FINISH_ALWAYS or FINISH_ADJACENT instead or refer to the current API.")
        this.filters = filters.toSet()
        this.placeholderIntent = placeholderIntent
        this.isSticky = isSticky
        this.finishPrimaryWithPlaceholder = finishPrimaryWithPlaceholder
    }

    /**
     * Builder for [SplitPlaceholderRule].
     *
     * @param filters See [SplitPlaceholderRule.filters].
     * @param placeholderIntent See [SplitPlaceholderRule.placeholderIntent].
     */
    class Builder(
        private val filters: Set<ActivityFilter>,
        private val placeholderIntent: Intent,
    ) {
        @IntRange(from = 0)
        private var minWidthDp: Int = DEFAULT_SPLIT_MIN_DIMENSION_DP
        @IntRange(from = 0)
        private var minSmallestWidthDp: Int = DEFAULT_SPLIT_MIN_DIMENSION_DP
        @SplitPlaceholderFinishBehavior
        private var finishPrimaryWithPlaceholder: Int = FINISH_ALWAYS
        private var isSticky: Boolean = false
        @FloatRange(from = 0.0, to = 1.0)
        private var splitRatio: Float = 0.5f
        @LayoutDirection
        private var layoutDirection: Int = LOCALE

        /**
         * @see SplitPlaceholderRule.minWidthDp
         */
        fun setMinWidthDp(@IntRange(from = 0) minWidthDp: Int): Builder =
            apply { this.minWidthDp = minWidthDp }

        /**
         * @see SplitPlaceholderRule.minSmallestWidthDp
         */
        fun setMinSmallestWidthDp(@IntRange(from = 0) minSmallestWidthDp: Int): Builder =
            apply { this.minSmallestWidthDp = minSmallestWidthDp }

        /**
         * @see SplitPlaceholderRule.finishPrimaryWithPlaceholder
         */
        fun setFinishPrimaryWithPlaceholder(
            @SplitPlaceholderFinishBehavior finishPrimaryWithPlaceholder: Int
        ): Builder =
            apply {
               this.finishPrimaryWithPlaceholder = finishPrimaryWithPlaceholder
            }

        /**
         * @see SplitPlaceholderRule.isSticky
         */
        fun setSticky(isSticky: Boolean): Builder =
            apply { this.isSticky = isSticky }

        /**
         * @see SplitPlaceholderRule.splitRatio
         */
        fun setSplitRatio(@FloatRange(from = 0.0, to = 1.0) splitRatio: Float): Builder =
            apply { this.splitRatio = splitRatio }

        /**
         * @see SplitPlaceholderRule.layoutDirection
         */
        fun setLayoutDirection(@LayoutDirection layoutDirection: Int): Builder =
            apply { this.layoutDirection = layoutDirection }

        fun build() = SplitPlaceholderRule(filters, placeholderIntent, isSticky,
            finishPrimaryWithPlaceholder, minWidthDp, minSmallestWidthDp, splitRatio,
            layoutDirection)
    }

    /**
     * Creates a new immutable instance by adding a filter to the set.
     * @see filters
     */
    internal operator fun plus(filter: ActivityFilter): SplitPlaceholderRule {
        val newSet = mutableSetOf<ActivityFilter>()
        newSet.addAll(filters)
        newSet.add(filter)
        return Builder(newSet.toSet(), placeholderIntent)
            .setMinWidthDp(minWidthDp)
            .setMinSmallestWidthDp(minSmallestWidthDp)
            .setSticky(isSticky)
            .setFinishPrimaryWithPlaceholder(finishPrimaryWithPlaceholder)
            .setSplitRatio(splitRatio)
            .setLayoutDirection(layoutDirection)
            .build()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SplitPlaceholderRule) return false
        if (!super.equals(other)) return false

        if (placeholderIntent != other.placeholderIntent) return false
        if (isSticky != other.isSticky) return false
        if (finishPrimaryWithPlaceholder != other.finishPrimaryWithPlaceholder) return false
        if (filters != other.filters) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + placeholderIntent.hashCode()
        result = 31 * result + isSticky.hashCode()
        result = 31 * result + finishPrimaryWithPlaceholder.hashCode()
        result = 31 * result + filters.hashCode()
        return result
    }
}