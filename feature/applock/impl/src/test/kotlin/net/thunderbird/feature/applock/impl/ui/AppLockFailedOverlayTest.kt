package net.thunderbird.feature.applock.impl.ui

import androidx.compose.ui.test.performClick
import app.k9mail.core.ui.compose.testing.ComposeTest
import app.k9mail.core.ui.compose.testing.onNodeWithText
import app.k9mail.core.ui.compose.testing.setContentWithTheme
import assertk.assertThat
import assertk.assertions.isEqualTo
import net.thunderbird.feature.applock.impl.R
import org.junit.Test

class AppLockFailedOverlayTest : ComposeTest() {

    @Test
    fun `should show error message when generic failure`() {
        val errorMessage = getString(R.string.applock_error_failed)

        setContentWithTheme {
            AppLockFailedOverlay(
                errorMessage = errorMessage,
                onRetryClick = {},
                onCloseClick = {},
            )
        }

        onNodeWithText(errorMessage).assertExists()
    }

    @Test
    fun `should show permanent lockout message`() {
        val errorMessage = getString(R.string.applock_error_lockout_permanent)

        setContentWithTheme {
            AppLockFailedOverlay(
                errorMessage = errorMessage,
                onRetryClick = {},
                onCloseClick = {},
            )
        }

        onNodeWithText(errorMessage, substring = true).assertExists()
    }

    @Test
    fun `should show temporary lockout message with duration`() {
        val durationSeconds = 30
        val errorMessage = org.robolectric.RuntimeEnvironment.getApplication().resources
            .getQuantityString(R.plurals.applock_error_lockout, durationSeconds, durationSeconds)

        setContentWithTheme {
            AppLockFailedOverlay(
                errorMessage = errorMessage,
                onRetryClick = {},
                onCloseClick = {},
            )
        }

        onNodeWithText("30", substring = true).assertExists()
    }

    @Test
    fun `should trigger onRetryClick callback when retry button clicked`() {
        var retryClickCount = 0

        setContentWithTheme {
            AppLockFailedOverlay(
                errorMessage = getString(R.string.applock_error_failed),
                onRetryClick = { retryClickCount++ },
                onCloseClick = {},
            )
        }

        onNodeWithText(R.string.applock_button_unlock).performClick()

        assertThat(retryClickCount).isEqualTo(1)
    }

    @Test
    fun `should trigger onCloseClick callback when close button clicked`() {
        var closeClickCount = 0

        setContentWithTheme {
            AppLockFailedOverlay(
                errorMessage = getString(R.string.applock_error_failed),
                onRetryClick = {},
                onCloseClick = { closeClickCount++ },
            )
        }

        onNodeWithText(R.string.applock_button_close_app).performClick()

        assertThat(closeClickCount).isEqualTo(1)
    }
}
