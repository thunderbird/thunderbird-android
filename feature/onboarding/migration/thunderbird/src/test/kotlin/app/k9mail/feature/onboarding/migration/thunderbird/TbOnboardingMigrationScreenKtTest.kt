package app.k9mail.feature.onboarding.migration.thunderbird

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import app.k9mail.core.common.provider.BrandNameProvider
import app.k9mail.core.ui.compose.testing.ComposeTest
import app.k9mail.core.ui.compose.testing.setContentWithTheme
import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class TbOnboardingMigrationScreenKtTest : ComposeTest() {
    @Test
    fun `pressing QrCodeImportButton should call onQrCodeScanClick`() = runComposeTest {
        var qrCodeScanClickCounter = 0
        var addAccountClickCounter = 0
        setContentWithTheme {
            TbOnboardingMigrationScreen(
                onQrCodeScanClick = { qrCodeScanClickCounter++ },
                onAddAccountClick = { addAccountClickCounter++ },
                brandNameProvider = FakeBrandNameProvider,
            )
        }

        composeTestRule.onNodeWithTag("QrCodeImportButton")
            .performScrollTo()
            .performClick()

        assertThat(qrCodeScanClickCounter).isEqualTo(1)
        assertThat(addAccountClickCounter).isEqualTo(0)
    }

    @Test
    fun `pressing AddAccountButton button should call onAddAccountClick`() = runComposeTest {
        var qrCodeScanClickCounter = 0
        var addAccountClickCounter = 0
        setContentWithTheme {
            TbOnboardingMigrationScreen(
                onQrCodeScanClick = { qrCodeScanClickCounter++ },
                onAddAccountClick = { addAccountClickCounter++ },
                brandNameProvider = FakeBrandNameProvider,
            )
        }

        composeTestRule.onNodeWithTag("AddAccountButton")
            .performScrollTo()
            .performClick()

        assertThat(addAccountClickCounter).isEqualTo(1)
        assertThat(qrCodeScanClickCounter).isEqualTo(0)
    }
}

private object FakeBrandNameProvider : BrandNameProvider {
    override val brandName = "Thunderbird"
}
