package app.k9mail.core.ui.compose.common.koin

import androidx.compose.runtime.Composable
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBody1
import app.k9mail.core.ui.compose.testing.ComposeTest
import app.k9mail.core.ui.compose.testing.onNodeWithText
import kotlin.test.Test
import org.koin.compose.koinInject

class KoinPreviewTest : ComposeTest() {
    @Test
    fun `koinPreview should make dependencies available in WithContent block`() = runComposeTest {
        val injectString = "Test"

        setContent {
            koinPreview {
                factory { injectString }
            } WithContent {
                TestComposable()
            }
        }

        onNodeWithText(injectString).assertExists()
    }
}

@Composable
private fun TestComposable(
    injected: String = koinInject(),
) {
    TextBody1(text = injected)
}
