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
    fun `pressing QrCodeImportButton should call onQrCodeScan`() = runComposeTest {
        var qrCodeScanClickCounter = 0
        setContentWithTheme {
            TbOnboardingMigrationScreen(
                onQrCodeScan = { qrCodeScanClickCounter++ },
                onAddAccount = { error("Should not be called") },
                onImport = { error("Should not be called") },
                brandNameProvider = FakeBrandNameProvider,
            )
        }

        composeTestRule.onNodeWithTag("QrCodeImportButton")
            .performScrollTo()
            .performClick()

        assertThat(qrCodeScanClickCounter).isEqualTo(1)
    }

    @Test
    fun `pressing AddAccountButton button should call onAddAccount`() = runComposeTest {
        var addAccountClickCounter = 0
        setContentWithTheme {
            TbOnboardingMigrationScreen(
                onQrCodeScan = { error("Should not be called") },
                onAddAccount = { addAccountClickCounter++ },
                onImport = { error("Should not be called") },
                brandNameProvider = FakeBrandNameProvider,
            )
        }

        composeTestRule.onNodeWithTag("AddAccountButton")
            .performScrollTo()
            .performClick()

        assertThat(addAccountClickCounter).isEqualTo(1)
    }

    @Test
    fun `pressing ImportButton button should call onImport`() = runComposeTest {
        var importClickCounter = 0
        setContentWithTheme {
            TbOnboardingMigrationScreen(
                onQrCodeScan = { error("Should not be called") },
                onAddAccount = { error("Should not be called") },
                onImport = { importClickCounter++ },
                brandNameProvider = FakeBrandNameProvider,
            )
        }

        composeTestRule.onNodeWithTag("ImportButton")
            .performScrollTo()
            .performClick()

        assertThat(importClickCounter).isEqualTo(1)
    }
}

private object FakeBrandNameProvider : BrandNameProvider {
    override val brandName = "Thunderbird"
}
