package app.k9mail.feature.migration.qrcode.ui

import android.app.Application
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import app.k9mail.core.ui.compose.testing.ComposeTest
import app.k9mail.core.ui.compose.testing.setContentWithTheme
import app.k9mail.feature.migration.qrcode.BuildConfig
import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.robolectric.annotation.Config

@Config(application = Application::class)
class QrCodeScannerBottomContentKtTest : ComposeTest() {
    init {
        // Running this test class in the release configuration fails with the following error message:
        // Unable to resolve activity for Intent { act=android.intent.action.MAIN cat=[android.intent.category.LAUNCHER]
        // cmp=app.k9mail.feature.migration.qrcode/androidx.activity.ComponentActivity } -- see
        // https://github.com/robolectric/robolectric/pull/4736 for details

        // So we make sure this test class is only run in the debug configuration.
        assumeTrue(BuildConfig.DEBUG)
    }

    @Test
    fun `not having a total count should not show ScannedStatus`() = runComposeTest {
        setContentWithTheme {
            QrCodeScannerBottomContent(
                scannedCount = 0,
                totalCount = 0,
                onDoneClick = {},
            )
        }

        composeTestRule.onNodeWithTag("ScannedStatus").assertDoesNotExist()
        composeTestRule.onNodeWithTag("DoneButton").assertExists()
    }

    @Test
    fun `having a total count should show ScannedStatus`() = runComposeTest {
        setContentWithTheme {
            QrCodeScannerBottomContent(
                scannedCount = 1,
                totalCount = 2,
                onDoneClick = {},
            )
        }

        composeTestRule.onNodeWithTag("ScannedStatus").assertTextContains("Scanned 1 of 2")
        composeTestRule.onNodeWithTag("DoneButton").assertExists()
    }

    @Test
    fun `clicking Done button should invoke onDoneClick`() = runComposeTest {
        var doneClickCount = 0

        setContentWithTheme {
            QrCodeScannerBottomContent(
                scannedCount = 0,
                totalCount = 0,
                onDoneClick = { doneClickCount++ },
            )
        }

        composeTestRule.onNodeWithTag("DoneButton").performClick()

        assertThat(doneClickCount).isEqualTo(1)
    }
}
