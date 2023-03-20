package app.k9mail.core.ui.compose.common.window

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class WindowSizeClassTest {

    @Test
    fun `should return compact when width is less than 600`() {
        val width = 599

        val windowSizeClass = WindowSizeClass.fromWidth(width)

        assertThat(windowSizeClass).isEqualTo(WindowSizeClass.Compact)
    }

    @Test
    fun `should return medium when width is 600`() {
        val width = 600

        val windowSizeClass = WindowSizeClass.fromWidth(width)

        assertThat(windowSizeClass).isEqualTo(WindowSizeClass.Medium)
    }

    @Test
    fun `should return medium when width is less than 840`() {
        val width = 839

        val windowSizeClass = WindowSizeClass.fromWidth(width)

        assertThat(windowSizeClass).isEqualTo(WindowSizeClass.Medium)
    }

    @Test
    fun `should return expanded when width is 840`() {
        val width = 840

        val windowSizeClass = WindowSizeClass.fromWidth(width)

        assertThat(windowSizeClass).isEqualTo(WindowSizeClass.Expanded)
    }

    @Test
    fun `should return compact when height is less than 480`() {
        val height = 479

        val windowSizeClass = WindowSizeClass.fromHeight(height)

        assertThat(windowSizeClass).isEqualTo(WindowSizeClass.Compact)
    }

    @Test
    fun `should return medium when height is 480`() {
        val height = 480

        val windowSizeClass = WindowSizeClass.fromHeight(height)

        assertThat(windowSizeClass).isEqualTo(WindowSizeClass.Medium)
    }

    @Test
    fun `should return medium when height is less than 900`() {
        val height = 899

        val windowSizeClass = WindowSizeClass.fromHeight(height)

        assertThat(windowSizeClass).isEqualTo(WindowSizeClass.Medium)
    }

    @Test
    fun `should return expanded when height is 900`() {
        val height = 900

        val windowSizeClass = WindowSizeClass.fromHeight(height)

        assertThat(windowSizeClass).isEqualTo(WindowSizeClass.Expanded)
    }
}
