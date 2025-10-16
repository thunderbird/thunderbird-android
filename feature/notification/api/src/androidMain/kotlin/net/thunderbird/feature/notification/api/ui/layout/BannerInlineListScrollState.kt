package net.thunderbird.feature.notification.api.ui.layout

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue
import kotlin.math.abs
import kotlin.math.roundToInt

internal class BannerInlineListScrollState(
    initialBannerInlineListHeight: Float = 0f,
    initialBannerInlineListHeightOffset: Float = 0f,
    initialContentOffset: Float = 0f,
    initialAccumulatedContentOffset: Float = 0f,
) {
    /**
     * The height of the inline banner list. This is used to calculate the offset of the banner
     * list when scrolling.
     */
    var bannerInlineListHeight by mutableFloatStateOf(initialBannerInlineListHeight)

    /**
     * The current offset of the banner inline list. This is used to adjust the position of the list
     * when it is scrolled.
     * The value is between -[bannerInlineListHeight] and 0.
     *
     * Example:
     * - If the banner inline list is fully visible, the value is 0.
     * - If the banner inline list is scrolled up by 100px, the value is -100.
     * - If the banner inline list is fully scrolled up (not visible), the value is -[bannerInlineListHeight].
     */
    var bannerInlineListHeightOffset by mutableFloatStateOf(initialBannerInlineListHeightOffset)

    /**
     * The current offset of the content. This is used to adjust the position of the content
     * when the banner inline list is scrolled.
     */
    var contentOffset by mutableFloatStateOf(initialContentOffset)

    /**
     * The total accumulated scroll offset of the content. This value is used to determine
     * if the banner list is off-screen and to adjust the content offset when the banner
     * list height changes.
     */
    var accumulatedContentOffset by mutableFloatStateOf(initialAccumulatedContentOffset)

    /**
     * Checks if the banner inline list is completely off-screen.
     *
     * This is true if either the current scroll offset ([contentOffset]) indicates scrolling away from the
     * initial position, or if the total accumulated offset ([accumulatedContentOffset]) exceeds the height
     * of the banner inline list.
     *
     * @return `true` if the banner inline list is off screen, `false` otherwise.
     */
    fun isBannerListOffScreen(): Boolean {
        return abs(contentOffset) > 0f ||
            abs(accumulatedContentOffset) > bannerInlineListHeight
    }

    /**
     * Adjusts the offset of the banner inline list.
     *
     * This function is called when the height of the banner inline list changes. It calculates the new
     * offset based on the previous height and the accumulated content offset.
     *
     * @param prevHeight The previous height of the banner inline list.
     */
    fun adjustOffset(prevHeight: Float) {
        val adjustedOffset = accumulatedContentOffset - prevHeight
        contentOffset = adjustedOffset
        bannerInlineListHeightOffset = adjustedOffset.coerceIn(
            minimumValue = -bannerInlineListHeight.toFloat(),
            maximumValue = 0f,
        )
    }

    /**
     * Calculates the extra padding needed for the content below the banner list.
     *
     * This padding ensures that the content is not obscured by the banner list when it's visible.
     * If the banner list is off-screen, no extra padding is needed.
     * Otherwise, the padding is the sum of the banner list height and its current offset.
     *
     * @return The calculated extra padding as a Float.
     */
    fun calculateExtraPadding(): Float {
        return if (isBannerListOffScreen()) {
            0f
        } else {
            bannerInlineListHeight + bannerInlineListHeightOffset
        }
    }

    /**
     * Calculates the Y offset for the inline banner list.
     *
     * This function determines the vertical position of the inline banner list based on the scaffold's top padding,
     * the global height of the banner, and the current offset of the inline list.
     *
     * @param scaffoldTopPadding The top padding of the scaffold, typically the height of the app bar.
     * @param bannerGlobalHeight The global height of the banner.
     * @return The calculated Y offset for the inline banner list, rounded to the nearest integer.
     */
    fun calculateInlineOffsetY(scaffoldTopPadding: Float, bannerGlobalHeight: Int): Int {
        val inlineBaseTop = scaffoldTopPadding + bannerGlobalHeight
        val offset = bannerInlineListHeightOffset
        return (inlineBaseTop + offset).roundToInt()
    }

    companion object Companion {
        val Saver: Saver<BannerInlineListScrollState, *> = listSaver(
            save = {
                listOf(
                    it.bannerInlineListHeight,
                    it.bannerInlineListHeightOffset,
                    it.contentOffset,
                    it.accumulatedContentOffset,
                )
            },
            restore = {
                BannerInlineListScrollState(
                    initialBannerInlineListHeight = it[0],
                    initialBannerInlineListHeightOffset = it[1],
                    initialContentOffset = it[2],
                    initialAccumulatedContentOffset = it[3],
                )
            },
        )
    }
}
