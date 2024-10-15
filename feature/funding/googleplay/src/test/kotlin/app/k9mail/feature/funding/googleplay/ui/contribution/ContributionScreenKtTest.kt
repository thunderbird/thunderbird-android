package app.k9mail.feature.funding.googleplay.ui.contribution

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.performClick
import app.k9mail.core.ui.compose.common.activity.LocalActivity
import app.k9mail.core.ui.compose.testing.ComposeTest
import app.k9mail.core.ui.compose.testing.onNodeWithTag
import app.k9mail.core.ui.compose.testing.pressBack
import app.k9mail.core.ui.compose.testing.setContentWithTheme
import app.k9mail.feature.funding.googleplay.ui.contribution.ContributionContract.State
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import org.mockito.Mockito.mock

internal class ContributionScreenKtTest : ComposeTest() {

    @Test
    fun `should call onBack when back button is pressed`() = runTest {
        val initialState = State()
        val viewModel = FakeContributionViewModel(initialState)
        var onBackCounter = 0

        setContentWithTheme {
            CompositionLocalProvider(LocalActivity provides mock()) {
                ContributionScreen(
                    onBack = { onBackCounter++ },
                    viewModel = viewModel,
                )
            }
        }

        assertThat(onBackCounter).isEqualTo(0)

        pressBack()

        assertThat(onBackCounter).isEqualTo(1)

        onNodeWithTag("TopAppBarBackButton").performClick()

        assertThat(onBackCounter).isEqualTo(2)
    }
}
