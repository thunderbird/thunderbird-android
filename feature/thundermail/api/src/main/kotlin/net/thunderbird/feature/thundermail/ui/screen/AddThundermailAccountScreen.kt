package net.thunderbird.feature.thundermail.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonDefaults
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonFilled
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonOutlined
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodySmall
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleLarge
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleSmall
import app.k9mail.core.ui.compose.designsystem.template.ResponsiveContent
import app.k9mail.core.ui.compose.designsystem.template.Scaffold
import net.thunderbird.core.ui.compose.common.modifier.testTagAsResourceId
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons
import net.thunderbird.core.ui.compose.theme2.MainTheme
import net.thunderbird.feature.thundermail.R
import net.thunderbird.feature.thundermail.ui.brandBackground
import net.thunderbird.feature.thundermail.ui.screen.AddThundermailAccountScreenDefaults.TEST_TAG_CONTENT_ROOT
import net.thunderbird.feature.thundermail.ui.screen.AddThundermailAccountScreenDefaults.TEST_TAG_SCAN_QR_CODE_BUTTON
import net.thunderbird.feature.thundermail.ui.screen.AddThundermailAccountScreenDefaults.TEST_TAG_SETUP_ANOTHER_ACCOUNT_BUTTON
import net.thunderbird.feature.thundermail.ui.screen.AddThundermailAccountScreenDefaults.TEST_TAG_SIGN_IN_WITH_THUNDERMAIL_BUTTON
import net.thunderbird.feature.thundermail.ui.screen.AddThundermailAccountScreenDefaults.TEST_TAG_WHAT_IS_THUNDERMAIL_LINK

@Composable
fun AddThundermailAccountScreen(
    header: @Composable ColumnScope.() -> Unit,
    onSignWithThundermailClick: () -> Unit,
    onScanQrCodeClick: () -> Unit,
    onSetupAnotherAccountClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier) { paddingValues ->
        ResponsiveContent(
            useSurfaceForExpandedContent = false,
            modifier = Modifier
                .fillMaxSize()
                .brandBackground()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
                .testTagAsResourceId(TEST_TAG_CONTENT_ROOT),
        ) { responsivePadding ->
            Column(
                modifier = Modifier
                    .padding(responsivePadding)
                    .verticalScroll(rememberScrollState())
                    .padding(MainTheme.spacings.quadruple),
                verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.quadruple),
            ) {
                Spacer(modifier = Modifier.weight(weight = .5f))
                header()

                TextTitleLarge(
                    text = stringResource(R.string.thundermail_add_account_title),
                    color = MainTheme.colors.primary,
                )
                ThundermailPanel(
                    onSignWithThundermailClick = onSignWithThundermailClick,
                    onScanQrCodeClick = onScanQrCodeClick,
                    modifier = Modifier.fillMaxWidth(),
                )
                ButtonOutlined(
                    text = stringResource(R.string.thundermail_setup_another_account_button),
                    onClick = onSetupAnotherAccountClick,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MainTheme.colors.primary,
                    ),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .testTagAsResourceId(TEST_TAG_SETUP_ANOTHER_ACCOUNT_BUTTON),
                )
                Spacer(modifier = Modifier.weight(weight = .5f))
            }
        }
    }
}

@Composable
private fun ThundermailPanel(
    onSignWithThundermailClick: () -> Unit,
    onScanQrCodeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MainTheme.colors.surfaceContainerLowest,
        shape = MainTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = MainTheme.spacings.double, vertical = MainTheme.spacings.triple),
            verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.double),
        ) {
            TextTitleMedium(
                text = stringResource(R.string.thundermail_sign_in_to_thundermail),
                color = MainTheme.colors.primary,
            )
            ButtonFilled(
                text = stringResource(R.string.thundermail_sign_in_with_thundermail_button),
                onClick = onSignWithThundermailClick,
                leadingIcon = {
                    Icon(imageVector = Icons.Filled.Thundermail, modifier = Modifier.size(MainTheme.sizes.iconSmall))
                    Spacer(modifier = Modifier.width(MainTheme.spacings.default))
                },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .testTagAsResourceId(TEST_TAG_SIGN_IN_WITH_THUNDERMAIL_BUTTON),
            )
            TextTitleSmall(
                text = stringResource(R.string.thundermail_or_divider),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            TextBodyMedium(text = stringResource(R.string.thundermail_add_your_account_copy))
            ButtonOutlined(
                text = stringResource(R.string.thundermail_scan_qr_code_button),
                onClick = onScanQrCodeClick,
                icon = Icons.Outlined.QrCode,
                colors = ButtonDefaults.outlinedButtonColors(
                    iconColor = MainTheme.colors.primary,
                    contentColor = MainTheme.colors.primary,
                ),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .testTagAsResourceId(TEST_TAG_SCAN_QR_CODE_BUTTON),
            )
            ThundermailLink()
        }
    }
}

@Composable
private fun ColumnScope.ThundermailLink(modifier: Modifier = Modifier) {
    TextBodySmall(
        text = buildAnnotatedString {
            pushLink(LinkAnnotation.Url("https://www.tb.pro/thundermail/"))
            withStyle(
                style = SpanStyle(
                    color = MainTheme.colors.primary,
                    textDecoration = TextDecoration.Underline,
                ),
            ) {
                append(stringResource(R.string.thundermail_what_is_thundermail))
            }
        },
        modifier = modifier
            .align(Alignment.CenterHorizontally)
            .testTagAsResourceId(TEST_TAG_WHAT_IS_THUNDERMAIL_LINK),
    )
}

internal object AddThundermailAccountScreenDefaults {
    const val TEST_TAG_CONTENT_ROOT = "AddThundermailAccountScreen_content"
    const val TEST_TAG_SCAN_QR_CODE_BUTTON = "AddThundermailAccountScreen_scanQrCodeButton"
    const val TEST_TAG_SIGN_IN_WITH_THUNDERMAIL_BUTTON = "AddThundermailAccountScreen_signInWithThundermailButton"
    const val TEST_TAG_SETUP_ANOTHER_ACCOUNT_BUTTON = "AddThundermailAccountScreen_setupAnotherAccountButton"
    const val TEST_TAG_WHAT_IS_THUNDERMAIL_LINK = "AddThundermailAccountScreen_whatIsThundermailLink"
}
