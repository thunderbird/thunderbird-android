package app.k9mail.feature.onboarding.migration.thunderbird

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.LineHeightStyle
import app.k9mail.core.common.provider.BrandNameProvider
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonFilled
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonOutlined
import app.k9mail.core.ui.compose.designsystem.atom.card.CardFilled
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium
import app.k9mail.core.ui.compose.designsystem.template.ResponsiveWidthContainer
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.feature.account.common.ui.AppTitleTopHeader
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.koin.compose.koinInject

@Composable
internal fun TbOnboardingMigrationScreen(
    onQrCodeScan: () -> Unit,
    onAddAccount: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
    brandNameProvider: BrandNameProvider = koinInject(),
) {
    val scrollState = rememberScrollState()

    ResponsiveWidthContainer(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
        ) {
            AppTitleTopHeader(
                title = brandNameProvider.brandName,
            )

            Spacer(
                modifier = Modifier
                    .height(MainTheme.spacings.double)
                    .weight(1f),
            )

            AlreadyUsingThunderbirdCard(onQrCodeScan)

            Spacer(modifier = Modifier.height(MainTheme.spacings.triple))

            TextGroup(title = stringResource(R.string.onboarding_migration_thunderbird_new_account_title)) {
                ButtonOutlined(
                    text = stringResource(R.string.onboarding_migration_thunderbird_new_account_button_text),
                    onClick = onAddAccount,
                    modifier = Modifier.testTag("AddAccountButton"),
                )
            }

            TextGroup(title = stringResource(R.string.onboarding_migration_thunderbird_import_title)) {
                ButtonOutlined(
                    text = stringResource(R.string.onboarding_migration_thunderbird_import_button_text),
                    onClick = onImport,
                    modifier = Modifier.testTag("ImportButton"),
                )
            }

            Spacer(
                modifier = Modifier
                    .height(MainTheme.spacings.double)
                    .weight(1f),
            )
        }
    }
}

@Composable
private fun AlreadyUsingThunderbirdCard(onQrCodeScan: () -> Unit) {
    TextCard(title = stringResource(R.string.onboarding_migration_thunderbird_qr_code_import_title)) {
        TextBodyMedium(
            text = stringResource(R.string.onboarding_migration_thunderbird_qr_code_import_text),
            modifier = Modifier
                .padding(bottom = MainTheme.spacings.double),
        )

        TextBodyMediumFullLineHeight(
            text = stringResource(R.string.onboarding_migration_thunderbird_qr_code_import_instructions_intro),
        )

        BulletList(
            items = persistentListOf(
                stringResource(R.string.onboarding_migration_thunderbird_qr_code_import_instructions_bullet_1),
                stringResource(R.string.onboarding_migration_thunderbird_qr_code_import_instructions_bullet_2),
            ),
            modifier = Modifier
                .padding(bottom = MainTheme.spacings.double),
        )

        ButtonFilled(
            text = stringResource(R.string.onboarding_migration_thunderbird_qr_code_import_button_text),
            onClick = onQrCodeScan,
            modifier = Modifier
                .testTag("QrCodeImportButton")
                .align(Alignment.CenterHorizontally),
        )
    }
}

@Composable
private fun TextCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    CardFilled(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MainTheme.spacings.quadruple),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MainTheme.spacings.double),
        ) {
            TextTitleMedium(
                text = title,
                color = MainTheme.colors.primary,
                modifier = Modifier
                    .padding(bottom = MainTheme.spacings.double),
            )

            content()
        }
    }
}

@Composable
private fun TextGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(MainTheme.spacings.double),
    ) {
        TextTitleMedium(
            text = title,
            color = MainTheme.colors.primary,
            modifier = Modifier
                .padding(bottom = MainTheme.spacings.default),
        )

        content()
    }
}

@Composable
private fun BulletList(
    items: ImmutableList<String>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        for (item in items) {
            Row {
                TextBodyMediumFullLineHeight(text = " \u2022 ")
                TextBodyMediumFullLineHeight(text = item)
            }
        }
    }
}

@Composable
private fun TextBodyMediumFullLineHeight(text: String) {
    // Disable line height trimming so that the space between TextBodyMediumFullLineHeight instances following each
    // other is the same as the space between lines of text inside a single TextBodyMedium.
    TextBodyMedium(
        text = text,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Proportional,
            trim = LineHeightStyle.Trim.None,
        ),
    )
}
