package net.thunderbird.components.ui.bolt.common.padding

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import net.thunderbird.components.ui.bolt.common.window.WindowWidthSizeClass
import net.thunderbird.components.ui.testing.ComposeUiTestHarness
import net.thunderbird.components.ui.testing.ComposeUiTestScope
import net.thunderbird.components.ui.testing.setContentWithWindowSize

class CalculateResponsivePaddingTest : ComposeUiTestHarness() {

    @Test
    fun `calculateResponsiveWidthPadding should return 0 horizontal padding for Small width size class`() =
        runComposeTest {
            val width = WindowWidthSizeClass.BREAKPOINT_SMALL - 1.dp

            val paddingValues = calculateResponsiveWidthPadding(width = width)

            assertHorizontalPaddingEquals(paddingValues = paddingValues, expected = 0.dp)
        }

    @Test
    fun `calculateResponsiveWidthPadding should return 0 horizontal padding for Compact width size class`() =
        runComposeTest {
            val width = WindowWidthSizeClass.BREAKPOINT_COMPACT - 1.dp

            val paddingValues = calculateResponsiveWidthPadding(width = width)

            assertHorizontalPaddingEquals(paddingValues = paddingValues, expected = 0.dp)
        }

    @Test
    fun `calculateResponsiveWidthPadding should return correct horizontal padding for Medium width size class`() =
        runComposeTest {
            val width = WindowWidthSizeClass.BREAKPOINT_MEDIUM - 1.dp
            val expectedPadding = (width - WindowWidthSizeClass.BREAKPOINT_COMPACT) / 2

            val paddingValues = calculateResponsiveWidthPadding(width = width)

            assertHorizontalPaddingEquals(paddingValues = paddingValues, expected = expectedPadding)
        }

    @Test
    fun `calculateResponsiveWidthPadding should return correct horizontal padding for Expanded width size class`() =
        runComposeTest {
            val width = WindowWidthSizeClass.BREAKPOINT_MEDIUM
            val expectedPadding = (width - WindowWidthSizeClass.BREAKPOINT_MEDIUM) / 2

            val paddingValues = calculateResponsiveWidthPadding(width = width)

            assertHorizontalPaddingEquals(paddingValues = paddingValues, expected = expectedPadding)
        }

    private fun ComposeUiTestScope.calculateResponsiveWidthPadding(width: Dp): PaddingValues {
        lateinit var paddingValues: PaddingValues

        setContentWithWindowSize(windowSize = DpSize(width = width, height = 600.dp)) {
            paddingValues = calculateResponsiveWidthPadding()
        }
        waitForIdle()

        return paddingValues
    }

    private fun assertHorizontalPaddingEquals(
        paddingValues: PaddingValues,
        expected: Dp,
    ) {
        assertThat(paddingValues.calculateLeftPadding(LayoutDirection.Ltr)).isEqualTo(expected)
        assertThat(paddingValues.calculateRightPadding(LayoutDirection.Ltr)).isEqualTo(expected)
    }
}
