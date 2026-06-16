package net.thunderbird.feature.thundermail.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import net.thunderbird.components.ui.bolt.atom.DividerHorizontal
import net.thunderbird.components.ui.bolt.atom.button.ButtonDefaults
import net.thunderbird.components.ui.bolt.atom.button.ButtonOutlined
import net.thunderbird.components.ui.bolt.atom.icon.Icons
import net.thunderbird.components.ui.bolt.atom.text.TextBodySmall
import net.thunderbird.components.ui.bolt.theme.MainTheme
import net.thunderbird.core.featureflag.FeatureFlagProvider
import net.thunderbird.feature.thundermail.R
import net.thunderbird.feature.thundermail.featureflag.ThundermailFeatureFlags
import org.koin.compose.koinInject

@Composable
fun ThundermailButtonPanel(
    onThundermailClick: () -> Unit,
    onScanQrCodeClick: () -> Unit,
    modifier: Modifier = Modifier,
    featureFlagProvider: FeatureFlagProvider = koinInject(),
) {
    if (featureFlagProvider.provide(ThundermailFeatureFlags.ThundermailOnboardingEnabled).isEnabled()) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.quadruple),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
            ) {
                ButtonOutlined(
                    text = stringResource(R.string.feature_thundermail_button_panel_sign_in),
                    onClick = onThundermailClick,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MainTheme.colors.primary,
                    ),
                    shape = ButtonDefaults.outlinedShape(
                        border = ButtonDefaults.outlinedButtonBorder(
                            color = MainTheme.colors.primary,
                        ),
                    ),
                    icon = Icons.Filled.Thundermail,
                )
                Spacer(modifier = Modifier.width(MainTheme.spacings.double))
                ButtonOutlined(
                    text = stringResource(R.string.feature_thundermail_button_panel_scan_qr_code),
                    onClick = onScanQrCodeClick,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MainTheme.colors.primary,
                    ),
                    shape = ButtonDefaults.outlinedShape(
                        border = ButtonDefaults.outlinedButtonBorder(
                            color = MainTheme.colors.primary,
                        ),
                    ),
                    icon = Icons.Outlined.QrCode,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.quarter),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DividerHorizontal(modifier = Modifier.weight(weight = 1f))
                TextBodySmall(
                    text = stringResource(R.string.feature_thundermail_button_panel_sign_in_or),
                    modifier = Modifier.weight(weight = 0.25f),
                    textAlign = TextAlign.Center,
                )
                DividerHorizontal(modifier = Modifier.weight(weight = 1f))
            }
        }
    }
}
