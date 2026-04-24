package net.thunderbird.feature.settings.import.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.Dp
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonDefaults
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonFilled
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonOutlined
import app.k9mail.core.ui.compose.designsystem.atom.card.CardDefaults
import app.k9mail.core.ui.compose.designsystem.atom.card.CardFilled
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodySmall
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium
import app.k9mail.feature.account.common.ui.AppTitleTopHeader
import app.k9mail.feature.account.common.ui.WizardNavigationBar
import app.k9mail.feature.account.common.ui.WizardNavigationBarState
import app.k9mail.feature.settings.importing.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.core.common.provider.BrandNameProvider
import net.thunderbird.core.ui.compose.common.modifier.testTagAsResourceId
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons
import net.thunderbird.core.ui.compose.theme2.MainTheme
import net.thunderbird.feature.settings.import.ui.ImportAccountScreenDefaults.TEST_TAG_IMPORT_ACCOUNT_IMPORT_BUTTON
import net.thunderbird.feature.settings.import.ui.ImportAccountScreenDefaults.TEST_TAG_IMPORT_ACCOUNT_QR_CODE_SCAN_BUTTON
import net.thunderbird.feature.settings.import.ui.ImportAccountScreenDefaults.TEST_TAG_IMPORT_ACCOUNT_ROOT
import net.thunderbird.feature.settings.import.ui.ImportAccountScreenDefaults.TEST_TAG_IMPORT_ACCOUNT_SELECT_FILE_BUTTON
import net.thunderbird.feature.thundermail.ui.brandBackground
import net.thunderbird.feature.thundermail.ui.component.template.ThundermailScaffold
import org.koin.compose.koinInject

@Composable
fun SharedTransitionScope.ImportAccountScreen(
    onQrCodeScanClick: () -> Unit,
    onSelectFileClick: () -> Unit,
    onImportClick: () -> Unit,
    onBack: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
    brandNameProvider: BrandNameProvider = koinInject(),
) {
    val scrollState = rememberScrollState()
    ThundermailScaffold(
        header = {
            AppTitleTopHeader(
                title = brandNameProvider.brandName,
                sharedTransitionScope = this,
                animatedVisibilityScope = animatedVisibilityScope,
            )
        },
        subHeaderText = stringResource(R.string.settings_import_account),
        bottomBar = { paddingValues, containerColor ->
            WizardNavigationBar(
                onNextClick = { },
                onBackClick = onBack,
                state = WizardNavigationBarState(showNext = false),
                modifier = Modifier
                    .imePadding()
                    .background(containerColor)
                    .padding(paddingValues)
                    .padding(top = MainTheme.spacings.default)
                    .padding(horizontal = MainTheme.spacings.quadruple),
            )
        },
        maxWidth = Dp.Unspecified,
        canScrollForward = scrollState.canScrollForward,
        modifier = modifier
            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.displayCutout)),
    ) { scaffoldPaddingValues, responsivePaddingValues, maxWidth ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .brandBackground()
                .padding(scaffoldPaddingValues)
                .padding(responsivePaddingValues)
                .testTagAsResourceId(TEST_TAG_IMPORT_ACCOUNT_ROOT),
        ) {
            AlreadyUsingThunderbirdCard(
                onQrCodeScan = onQrCodeScanClick,
                modifier = Modifier.padding(horizontal = MainTheme.spacings.double),
            )
            Spacer(modifier = Modifier.height(MainTheme.spacings.quadruple))
            TextGroup(title = stringResource(R.string.settings_import_thunderbird_import_title)) {
                MovingFromAnotherDeviceButton(
                    text = stringResource(R.string.settings_import_pick_document_button),
                    onClick = onSelectFileClick,
                    modifier = Modifier.testTagAsResourceId(TEST_TAG_IMPORT_ACCOUNT_SELECT_FILE_BUTTON),
                )
                MovingFromAnotherDeviceButton(
                    text = stringResource(R.string.settings_import_thunderbird_import_button_text),
                    onClick = onImportClick,
                    modifier = Modifier.testTagAsResourceId(TEST_TAG_IMPORT_ACCOUNT_IMPORT_BUTTON),
                )
            }
        }
    }
}

@Composable
fun MovingFromAnotherDeviceButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ButtonOutlined(
        text = text,
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MainTheme.colors.primary),
        shape = ButtonDefaults.outlinedShape(
            border = ButtonDefaults.outlinedButtonBorder(color = MainTheme.colors.outline),
        ),
        contentPadding = PaddingValues(
            vertical = MainTheme.spacings.oneHalf,
            horizontal = MainTheme.spacings.triple,
        ),
    )
}

@Composable
private fun AlreadyUsingThunderbirdCard(onQrCodeScan: () -> Unit, modifier: Modifier = Modifier) {
    TextCard(
        title = stringResource(R.string.settings_import_thunderbird_qr_code_import_title),
        modifier = modifier,
    ) {
        TextBodyMedium(
            text = stringResource(R.string.settings_import_thunderbird_qr_code_import_text),
        )

        Column {
            TextBodyMedium(
                text = stringResource(R.string.settings_import_thunderbird_qr_code_import_instructions_intro),
            )
            BulletList(
                items = persistentListOf(
                    stringResource(R.string.settings_import_thunderbird_qr_code_import_instructions_bullet_1),
                    stringResource(R.string.settings_import_thunderbird_qr_code_import_instructions_bullet_2_v2),
                ),
                modifier = Modifier.padding(top = MainTheme.spacings.half),
            )
        }

        ButtonFilled(
            text = stringResource(R.string.settings_import_scan_qr_code_button),
            onClick = onQrCodeScan,
            modifier = Modifier
                .testTagAsResourceId(TEST_TAG_IMPORT_ACCOUNT_QR_CODE_SCAN_BUTTON)
                .align(Alignment.CenterHorizontally),
            leadingIcon = {
                Icon(Icons.Outlined.QrCode)
                Spacer(modifier = Modifier.width(MainTheme.spacings.default))
            },
            contentPadding = PaddingValues(
                top = MainTheme.spacings.oneHalf,
                bottom = MainTheme.spacings.oneHalf,
                start = MainTheme.spacings.double,
                end = MainTheme.spacings.triple,
            ),
        )

        ThunderbirdVersionNote(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = MainTheme.spacings.half),
        )
    }
}

@Composable
private fun ThunderbirdVersionNote(
    modifier: Modifier = Modifier,
) {
    TextBodySmall(
        text = buildAnnotatedString {
            val formatString =
                stringResource(R.string.settings_import_thunderbird_qr_code_import_instructions_require_latest)
            val prefix = formatString.substringBefore("%s")
            val suffix = formatString.substringAfter("%s")
            append(prefix)
            pushLink(LinkAnnotation.Url(stringResource(R.string.settings_import_thunderbird_update_thunderbird_url)))
            append(
                stringResource(R.string.settings_import_thunderbird_qr_code_import_instructions_learn_update),
            )
            append(suffix)
        },
        modifier = modifier.padding(MainTheme.spacings.default),
    )
}

@Composable
private fun TextCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    CardFilled(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MainTheme.colors.surfaceContainerLowest),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MainTheme.spacings.double, vertical = MainTheme.spacings.triple),
            verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.double),
        ) {
            TextTitleMedium(text = title, color = MainTheme.colors.primary)

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
        verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.double),
        modifier = Modifier
            .fillMaxWidth()
            .padding(MainTheme.spacings.double),
    ) {
        TextTitleMedium(
            text = title,
            color = MainTheme.colors.primary,
        )
        content()
    }
}

@Composable
private fun BulletList(
    items: ImmutableList<String>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.half),
    ) {
        for (item in items) {
            Row {
                TextBodyMedium(text = " \u2022 ")
                TextBodyMedium(text = item)
            }
        }
    }
}

internal object ImportAccountScreenDefaults {
    const val TEST_TAG_IMPORT_ACCOUNT_ROOT = "ImportAccountScreen_root"
    const val TEST_TAG_IMPORT_ACCOUNT_QR_CODE_SCAN_BUTTON = "QrCodeImportButton"
    const val TEST_TAG_IMPORT_ACCOUNT_SELECT_FILE_BUTTON = "ImportAccountScreen_selectFileButton"
    const val TEST_TAG_IMPORT_ACCOUNT_IMPORT_BUTTON = "ImportButton"
}
