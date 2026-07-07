package net.thunderbird.feature.thundermail.internal.common.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import app.k9mail.feature.account.common.ui.AppTitleTopHeader
import net.thunderbird.components.ui.bolt.atom.button.ButtonDefaults
import net.thunderbird.components.ui.bolt.atom.button.ButtonFilled
import net.thunderbird.components.ui.bolt.atom.button.ButtonOutlined
import net.thunderbird.components.ui.bolt.atom.icon.Icons
import net.thunderbird.components.ui.bolt.atom.text.TextBodySmall
import net.thunderbird.components.ui.bolt.atom.text.TextTitleLarge
import net.thunderbird.components.ui.bolt.atom.text.TextTitleSmall
import net.thunderbird.components.ui.bolt.template.ResponsiveWidthContainer
import net.thunderbird.components.ui.bolt.template.Scaffold
import net.thunderbird.components.ui.bolt.theme.BoltTheme
import net.thunderbird.feature.thundermail.internal.common.R
import net.thunderbird.feature.thundermail.R as ThundermailApiR

@Composable
fun ThundermailAddAccountScreen(
    appTitle: String,
    onAddAccountClick: () -> Unit,
    onSignInWithThundermail: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
    ) { paddingValues ->
        ResponsiveWidthContainer(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
                .imePadding(),
        ) { responsiveWidthPadding ->
            Column(modifier = Modifier.verticalScroll(state = rememberScrollState())) {
                // The responsive width padding is applied to the action buttons only to avoid
                // doubling up the insets and collapsing the header on very wide windows (e.g. tablet,
                // desktop, etc).
                AppTitleTopHeader(title = appTitle) {
                    Spacer(modifier = Modifier.height(BoltTheme.spacings.triple))
                    TextTitleLarge(
                        text = stringResource(R.string.thundermail_add_account),
                        color = BoltTheme.colors.primary,
                        modifier = Modifier.padding(
                            start = BoltTheme.spacings.default,
                            end = BoltTheme.spacings.quadruple,
                        ),
                    )
                }
                Spacer(modifier = Modifier.height(BoltTheme.spacings.quadruple + BoltTheme.spacings.double))
                ButtonsPanel(
                    onAddAccountClick = onAddAccountClick,
                    onSignInWithThundermail = onSignInWithThundermail,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(responsiveWidthPadding),
                )
            }
        }
    }
}

@Composable
private fun ButtonsPanel(
    onAddAccountClick: () -> Unit,
    onSignInWithThundermail: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(BoltTheme.spacings.double),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        ButtonFilled(
            text = stringResource(R.string.thundermail_add_an_email_account),
            onClick = onAddAccountClick,
        )
        TextTitleSmall(
            text = buildAnnotatedString {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(stringResource(R.string.thundermail_or_divider))
                }
            },
        )
        ButtonOutlined(
            text = stringResource(ThundermailApiR.string.feature_thundermail_button_panel_sign_in),
            onClick = onSignInWithThundermail,
            icon = Icons.Filled.Thundermail,
            colors = ButtonDefaults.outlinedButtonColors(
                iconColor = BoltTheme.colors.primary,
                contentColor = BoltTheme.colors.primary,
            ),
        )
        TextBodySmall(
            text = buildAnnotatedString {
                withLink(
                    link = LinkAnnotation.Url("https://www.tb.pro/thundermail"),
                ) {
                    append(stringResource(R.string.thundermail_what_is_thundermail))
                }
            },
        )
    }
}
