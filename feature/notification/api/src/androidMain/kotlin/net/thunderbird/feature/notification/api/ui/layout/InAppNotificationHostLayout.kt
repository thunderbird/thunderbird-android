package net.thunderbird.feature.notification.api.ui.layout

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.SubcomposeMeasureScope
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxBy
import androidx.compose.ui.util.fastSumBy
import kotlin.math.roundToInt

@Composable
internal fun InAppNotificationHostLayout(
    behaviour: BannerInlineListScrollBehaviour,
    scaffoldPaddingValues: PaddingValues,
    bannerGlobal: @Composable () -> Unit,
    bannerInlineList: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    var bannerInlineListHeight by remember { mutableFloatStateOf(0f) }
    val scrollState = behaviour.state

    LaunchedEffect(bannerInlineListHeight) {
        // Update the banner inline list height whenever it changes.
        if (bannerInlineListHeight > 0) {
            val prevHeight = scrollState.bannerInlineListHeight
            scrollState.bannerInlineListHeight = bannerInlineListHeight
            // If any scroll is already present when the banner inline list gets displayed,
            // we add that offset to the height offset.
            if (scrollState.isBannerListOffScreen()) {
                scrollState.adjustOffset(prevHeight)
            }
        }
    }

    SubcomposeLayout(
        modifier = modifier.nestedScroll(behaviour.connection),
    ) { constraints ->
        val layoutWidth = constraints.maxWidth
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)

        // Measuring Banner Global
        val bannerGlobalPlaceables = subcomposeAndMeasureBannerGlobal(bannerGlobal, looseConstraints)
        val bannerGlobalHeight = bannerGlobalPlaceables.fastMaxBy { it.height }?.height ?: 0

        // Measuring Banner Inline list
        val bannerInlineListPlaceables = subcomposeAndMeasureBannerInlineList(bannerInlineList, looseConstraints)
        bannerInlineListHeight = bannerInlineListPlaceables.fastSumBy { it.height }.toFloat()

        // Measuring Body content, applying scaffold paddings
        val mainContentPlaceables = subcomposeAndMeasureMainContent(
            bannerGlobalHeight = bannerGlobalHeight,
            scrollState = scrollState,
            scaffoldPaddingValues = scaffoldPaddingValues,
            content = content,
            looseConstraints = looseConstraints,
        )
        val mainContentHeight = mainContentPlaceables.fastSumBy { it.height }

        // In case the maxHeight is not defined (for example when no content is passed to the content lambda),
        // we manually calculate the layout height to avoid a crash caused by the pre-condition check
        // of the layout function.
        val layoutHeight = when {
            constraints.minHeight == 0 ||
                constraints.maxHeight == Constraints.Infinity -> {
                constraints.constrainHeight(
                    bannerGlobalHeight + bannerInlineListHeight.roundToInt() + mainContentHeight,
                )
            }

            else -> {
                constraints.maxHeight
            }
        }

        layout(layoutWidth, layoutHeight) {
            // Set the body at 0, 0 as we are applying the positioning via
            // PaddingValues.
            mainContentPlaceables.fastForEach { it.placeRelative(x = 0, y = 0) }

            // Move inline list, following the height's offset
            val scaffoldTopPx = scaffoldPaddingValues.calculateTopPadding().toPx()
            // To avoid the layout to jump when a new banner inline is displayed
            // we only place the banner inline list if it is not off screen.
            if (!scrollState.isBannerListOffScreen()) {
                bannerInlineListPlaceables.fastForEach {
                    it.placeRelative(
                        x = 0,
                        y = scrollState.calculateInlineOffsetY(
                            scaffoldTopPadding = scaffoldTopPx,
                            bannerGlobalHeight = bannerGlobalHeight,
                        ),
                    )
                }
            }

            // Set fixed Banner Global
            bannerGlobalPlaceables.fastForEach { it.placeRelative(x = 0, y = scaffoldTopPx.roundToInt()) }
        }
    }
}

/**
 * Subcompose and Measure the Banner Global.
 */
private fun SubcomposeMeasureScope.subcomposeAndMeasureBannerGlobal(
    bannerGlobal: @Composable (() -> Unit),
    looseConstraints: Constraints,
): List<Placeable> = subcompose(
    slotId = InAppNotificationScaffoldContent.BannerGlobal,
    content = bannerGlobal,
).fastMap { it.measure(looseConstraints) }

/**
 * Subcompose and Measure the Banner Inline List.
 */
private fun SubcomposeMeasureScope.subcomposeAndMeasureBannerInlineList(
    bannerInlineList: @Composable (() -> Unit),
    looseConstraints: Constraints,
): List<Placeable> = subcompose(
    slotId = InAppNotificationScaffoldContent.BannerInlineList,
    content = bannerInlineList,
).fastMap { it.measure(looseConstraints) }

/**
 * Subcompose and measure the main content of the scaffold. It calculates the scroll offset
 * and applies it to the main content via [PaddingValues], taking in consideration:
 *
 * - The Banner Global Height, if visible
 * - The Banner Inline List Height, if visible
 * - The Scaffold Padding Values.
 */
private fun SubcomposeMeasureScope.subcomposeAndMeasureMainContent(
    bannerGlobalHeight: Int,
    scrollState: BannerInlineListScrollState,
    scaffoldPaddingValues: PaddingValues,
    content: @Composable ((PaddingValues) -> Unit),
    looseConstraints: Constraints,
): List<Placeable> = subcompose(
    slotId = InAppNotificationScaffoldContent.MainContent,
) {
    val layoutDirection = (this).layoutDirection
    val extraTopPadding = (bannerGlobalHeight + scrollState.calculateExtraPadding()).toDp()
    val innerPadding = PaddingValues(
        top = scaffoldPaddingValues.calculateTopPadding() + extraTopPadding,
        bottom = scaffoldPaddingValues.calculateBottomPadding(),
        start = scaffoldPaddingValues.calculateStartPadding(layoutDirection),
        end = scaffoldPaddingValues.calculateEndPadding(layoutDirection),
    )

    content(innerPadding)
}.fastMap { it.measure(looseConstraints) }

private enum class InAppNotificationScaffoldContent {
    BannerGlobal,
    BannerInlineList,
    MainContent,
}
