package net.thunderbird.core.ui.common.window

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import kotlin.test.Test

class WindowSizeClassTest {

    @Test
    fun `should return small when width is less than 350`() {
        val width = 349.dp

        val widthSizeClass = WindowWidthSizeClass.fromWidth(width)

        assertThat(widthSizeClass).isEqualTo(WindowWidthSizeClass.Small)
    }

    @Test
    fun `should return compact when width is 350`() {
        val width = 350.dp

        val widthSizeClass = WindowWidthSizeClass.fromWidth(width)

        assertThat(widthSizeClass).isEqualTo(WindowWidthSizeClass.Compact)
    }

    @Test
    fun `should return compact when width is less than 600`() {
        val width = 599.dp

        val widthSizeClass = WindowWidthSizeClass.fromWidth(width)

        assertThat(widthSizeClass).isEqualTo(WindowWidthSizeClass.Compact)
    }

    @Test
    fun `should return medium when width is 600`() {
        val width = 600.dp

        val widthSizeClass = WindowWidthSizeClass.fromWidth(width)

        assertThat(widthSizeClass).isEqualTo(WindowWidthSizeClass.Medium)
    }

    @Test
    fun `should return medium when width is less than 840`() {
        val width = 839.dp

        val widthSizeClass = WindowWidthSizeClass.fromWidth(width)

        assertThat(widthSizeClass).isEqualTo(WindowWidthSizeClass.Medium)
    }

    @Test
    fun `should return expanded when width is 840`() {
        val width = 840.dp

        val widthSizeClass = WindowWidthSizeClass.fromWidth(width)

        assertThat(widthSizeClass).isEqualTo(WindowWidthSizeClass.Expanded)
    }

    @Test
    fun `should throw exception for negative width`() {
        assertFailure {
            WindowWidthSizeClass.fromWidth((-1).dp)
        }.hasMessage("Width must be positive")
    }

    @Test
    fun `should return small when height is less than 350`() {
        val height = 349.dp

        val heightSizeClass = WindowHeightSizeClass.fromHeight(height)

        assertThat(heightSizeClass).isEqualTo(WindowHeightSizeClass.Small)
    }

    @Test
    fun `should return compact when height is 350`() {
        val height = 350.dp

        val heightSizeClass = WindowHeightSizeClass.fromHeight(height)

        assertThat(heightSizeClass).isEqualTo(WindowHeightSizeClass.Compact)
    }

    @Test
    fun `should return compact when height is less than 480`() {
        val height = 479.dp

        val heightSizeClass = WindowHeightSizeClass.fromHeight(height)

        assertThat(heightSizeClass).isEqualTo(WindowHeightSizeClass.Compact)
    }

    @Test
    fun `should return medium when height is 480`() {
        val height = 480.dp

        val heightSizeClass = WindowHeightSizeClass.fromHeight(height)

        assertThat(heightSizeClass).isEqualTo(WindowHeightSizeClass.Medium)
    }

    @Test
    fun `should return medium when height is less than 900`() {
        val height = 899.dp

        val heightSizeClass = WindowHeightSizeClass.fromHeight(height)

        assertThat(heightSizeClass).isEqualTo(WindowHeightSizeClass.Medium)
    }

    @Test
    fun `should return expanded when height is 900`() {
        val height = 900.dp

        val heightSizeClass = WindowHeightSizeClass.fromHeight(height)

        assertThat(heightSizeClass).isEqualTo(WindowHeightSizeClass.Expanded)
    }

    @Test
    fun `should throw exception for negative height`() {
        assertFailure {
            WindowHeightSizeClass.fromHeight((-1).dp)
        }.hasMessage("Height must be positive")
    }

    @Test
    fun `should return correct size class when calculateFromSize is called`() {
        val size = DpSize(width = 600.dp, height = 480.dp)

        val windowSizeClass = WindowSizeClass.calculateFromSize(size)

        assertThat(windowSizeClass.widthSizeClass).isEqualTo(WindowWidthSizeClass.Medium)
        assertThat(windowSizeClass.heightSizeClass).isEqualTo(WindowHeightSizeClass.Medium)
    }
}
