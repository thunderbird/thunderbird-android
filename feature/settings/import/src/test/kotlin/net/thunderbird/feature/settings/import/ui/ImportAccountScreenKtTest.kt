package net.thunderbird.feature.settings.import.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import app.k9mail.core.ui.compose.testing.ComposeTest
import app.k9mail.core.ui.compose.testing.setContentWithKoinAndTheme
import assertk.assertThat
import assertk.assertions.isEqualTo
import net.thunderbird.core.common.provider.BrandNameProvider
import net.thunderbird.core.common.provider.BrandTypographyProvider
import net.thunderbird.feature.settings.import.ui.ImportAccountScreenDefaults.TEST_TAG_IMPORT_ACCOUNT_IMPORT_BUTTON
import net.thunderbird.feature.settings.import.ui.ImportAccountScreenDefaults.TEST_TAG_IMPORT_ACCOUNT_QR_CODE_SCAN_BUTTON
import net.thunderbird.feature.settings.import.ui.ImportAccountScreenDefaults.TEST_TAG_IMPORT_ACCOUNT_SELECT_FILE_BUTTON
import net.thunderbird.feature.thundermail.ui.BrandBackgroundModifierProvider
import org.junit.Test

class ImportAccountScreenKtTest : ComposeTest() {
    @Test
    fun `pressing QrCodeImportButton should call onQrCodeScanClick`() = runComposeTest {
        var qrCodeScanClickCounter = 0
        setContentWithKoinAndTheme(
            modules = {
                single<BrandTypographyProvider> { BrandTypographyProvider {} }
                single { BrandBackgroundModifierProvider { Modifier } }
            },
        ) {
            SharedTransitionLayout {
                AnimatedVisibility(true) {
                    ImportAccountScreen(
                        onQrCodeScanClick = { qrCodeScanClickCounter++ },
                        onSelectFileClick = { error("Should not be called") },
                        onImportClick = { error("Should not be called") },
                        onBack = { error("Should not be called") },
                        brandNameProvider = FakeBrandNameProvider,
                        animatedVisibilityScope = this,
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag(TEST_TAG_IMPORT_ACCOUNT_QR_CODE_SCAN_BUTTON)
            .performScrollTo()
            .performClick()

        assertThat(qrCodeScanClickCounter).isEqualTo(1)
    }

    @Test
    fun `pressing Select file button should call onSelectFileClick`() = runComposeTest {
        var onSelectFileClickCounter = 0
        setContentWithKoinAndTheme(
            modules = {
                single<BrandTypographyProvider> { BrandTypographyProvider {} }
                single<BrandBackgroundModifierProvider> { BrandBackgroundModifierProvider { Modifier } }
            },
        ) {
            SharedTransitionLayout {
                AnimatedVisibility(true) {
                    ImportAccountScreen(
                        onQrCodeScanClick = { error("Should not be called") },
                        onSelectFileClick = { onSelectFileClickCounter++ },
                        onImportClick = { error("Should not be called") },
                        onBack = { error("Should not be called") },
                        brandNameProvider = FakeBrandNameProvider,
                        animatedVisibilityScope = this,
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag(TEST_TAG_IMPORT_ACCOUNT_SELECT_FILE_BUTTON)
            .performScrollTo()
            .performClick()

        assertThat(onSelectFileClickCounter).isEqualTo(1)
    }

    @Test
    fun `pressing ImportButton button should call onImportClick`() = runComposeTest {
        var importClickCounter = 0
        setContentWithKoinAndTheme(
            modules = {
                single<BrandTypographyProvider> { BrandTypographyProvider {} }
                single { BrandBackgroundModifierProvider { Modifier } }
            },
        ) {
            SharedTransitionLayout {
                AnimatedVisibility(true) {
                    ImportAccountScreen(
                        onQrCodeScanClick = { error("Should not be called") },
                        onSelectFileClick = { error("Should not be called") },
                        onImportClick = { importClickCounter++ },
                        onBack = { error("Should not be called") },
                        brandNameProvider = FakeBrandNameProvider,
                        animatedVisibilityScope = this,
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag(TEST_TAG_IMPORT_ACCOUNT_IMPORT_BUTTON)
            .performScrollTo()
            .performClick()

        assertThat(importClickCounter).isEqualTo(1)
    }

    private object FakeBrandNameProvider : BrandNameProvider {
        override val brandName = "Thunderbird"
    }
}
