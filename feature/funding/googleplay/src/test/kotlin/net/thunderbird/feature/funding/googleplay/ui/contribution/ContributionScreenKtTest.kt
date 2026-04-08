package net.thunderbird.feature.funding.googleplay.ui.contribution

import androidx.compose.ui.test.performClick
import app.k9mail.core.ui.compose.testing.ComposeTest
import app.k9mail.core.ui.compose.testing.onNodeWithTag
import app.k9mail.core.ui.compose.testing.pressBack
import app.k9mail.core.ui.compose.testing.setContentWithTheme
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import net.thunderbird.feature.funding.googleplay.ui.contribution.ContributionContract.State

internal class ContributionScreenKtTest : ComposeTest() {

    @Test
    fun `should call onBack when back button is pressed`() = runTest {
        // Arrange
        val initialState = State()
        val viewModel = FakeContributionViewModel(initialState)
        var onBackCounter = 0

        setContentWithTheme {
            ContributionScreen(
                onBack = { onBackCounter++ },
                viewModel = viewModel,
            )
        }

        // Assert (initial)
        assertThat(onBackCounter).isEqualTo(0)

        // Act (system back)
        pressBack()

        // Assert (after system back)
        assertThat(onBackCounter).isEqualTo(1)

        // Act (top app bar back button)
        onNodeWithTag("TopAppBarBackButton").performClick()

        // Assert (after top app bar back)
        assertThat(onBackCounter).isEqualTo(2)
    }
}
