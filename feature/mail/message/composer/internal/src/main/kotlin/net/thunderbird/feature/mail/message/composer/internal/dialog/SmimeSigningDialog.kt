package net.thunderbird.feature.mail.message.composer.internal.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import net.thunderbird.components.ui.bolt.atom.Checkbox
import net.thunderbird.components.ui.bolt.atom.button.ButtonFilled
import net.thunderbird.components.ui.bolt.atom.button.ButtonText
import net.thunderbird.components.ui.bolt.atom.card.CardOutlined
import net.thunderbird.components.ui.bolt.atom.icon.Icon
import net.thunderbird.components.ui.bolt.atom.icon.Icons
import net.thunderbird.components.ui.bolt.atom.text.TextBodyMedium
import net.thunderbird.components.ui.bolt.atom.text.TextBodySmall
import net.thunderbird.components.ui.bolt.atom.text.TextTitleSmall
import net.thunderbird.components.ui.bolt.organism.BasicDialog
import net.thunderbird.components.ui.bolt.theme.BoltTheme
import net.thunderbird.feature.mail.message.composer.internal.R
import net.thunderbird.feature.mail.message.composer.smime.SmimeCertificateInfo

@Suppress("LongMethod")
@Composable
internal fun SmimeSigningDialog(
    selectedAlias: String?,
    certificateInfo: SmimeCertificateInfo?,
    setAsDefault: Boolean,
    isSigningEnabledForMessage: Boolean,
    isFirstTimeSendPrompt: Boolean,
    showNoCertificateHint: Boolean,
    onChooseCertificateClick: () -> Unit,
    onSetAsDefaultChange: (Boolean) -> Unit,
    onConfirmSignClick: () -> Unit,
    onSendUnsignedClick: () -> Unit,
    onDisableSigningClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val headlineText = if (isFirstTimeSendPrompt) {
        stringResource(R.string.smime_signing_dialog_first_send_title)
    } else {
        stringResource(R.string.smime_signing_dialog_title)
    }
    val supportingText = if (isFirstTimeSendPrompt) {
        stringResource(R.string.smime_signing_dialog_first_send_supporting_text)
    } else {
        stringResource(R.string.smime_signing_dialog_supporting_text)
    }

    BasicDialog(
        headlineText = headlineText,
        supportingText = supportingText,
        content = {
            SmimeSigningDialogContent(
                selectedAlias = selectedAlias,
                certificateInfo = certificateInfo,
                setAsDefault = setAsDefault,
                showNoCertificateHint = showNoCertificateHint,
                onChooseCertificateClick = onChooseCertificateClick,
                onSetAsDefaultChange = onSetAsDefaultChange,
            )
        },
        buttons = {
            SmimeSigningDialogButtons(
                hasCertificate = !selectedAlias.isNullOrBlank(),
                isCertificateExpired = certificateInfo?.isExpired == true,
                isSigningEnabledForMessage = isSigningEnabledForMessage,
                isFirstTimeSendPrompt = isFirstTimeSendPrompt,
                onChooseCertificateClick = onChooseCertificateClick,
                onConfirmSignClick = onConfirmSignClick,
                onSendUnsignedClick = onSendUnsignedClick,
                onDisableSigningClick = onDisableSigningClick,
                onDismiss = onDismiss,
            )
        },
        onDismissRequest = onDismiss,
        contentPadding = PaddingValues(
            horizontal = BoltTheme.spacings.triple,
            vertical = BoltTheme.spacings.default,
        ),
        modifier = modifier,
    )
}

@Composable
private fun SmimeSigningDialogContent(
    selectedAlias: String?,
    certificateInfo: SmimeCertificateInfo?,
    setAsDefault: Boolean,
    showNoCertificateHint: Boolean,
    onChooseCertificateClick: () -> Unit,
    onSetAsDefaultChange: (Boolean) -> Unit,
) {
    val hasCertificate = !selectedAlias.isNullOrBlank()
    val isExpired = certificateInfo?.isExpired == true

    Column(
        verticalArrangement = Arrangement.spacedBy(BoltTheme.spacings.double),
        modifier = Modifier.fillMaxWidth(),
    ) {
        CertificateSelectionCard(
            selectedAlias = selectedAlias,
            certificateInfo = certificateInfo,
            onClick = onChooseCertificateClick,
        )

        if (showNoCertificateHint && !hasCertificate) {
            TextBodySmall(
                text = stringResource(R.string.smime_signing_dialog_no_certificate_hint),
                color = BoltTheme.colors.onSurfaceVariant,
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BoltTheme.spacings.default),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = hasCertificate && !isExpired) {
                    onSetAsDefaultChange(!setAsDefault)
                },
        ) {
            Checkbox(
                checked = setAsDefault && hasCertificate && !isExpired,
                onCheckedChange = onSetAsDefaultChange,
                enabled = hasCertificate && !isExpired,
            )
            TextBodyMedium(
                text = stringResource(R.string.smime_signing_dialog_set_default_checkbox),
                color = if (hasCertificate && !isExpired) {
                    BoltTheme.colors.onSurface
                } else {
                    BoltTheme.colors.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
private fun CertificateSelectionCard(
    selectedAlias: String?,
    certificateInfo: SmimeCertificateInfo?,
    onClick: () -> Unit,
) {
    val hasCertificate = !selectedAlias.isNullOrBlank()
    val isExpired = certificateInfo?.isExpired == true
    val icon = when {
        isExpired -> Icons.Outlined.Warning
        hasCertificate -> Icons.Outlined.CheckCircle
        else -> Icons.Outlined.Security
    }
    val iconTint = when {
        isExpired -> BoltTheme.colors.error
        hasCertificate -> BoltTheme.colors.primary
        else -> BoltTheme.colors.onSurfaceVariant
    }
    val issuerDisplayName = certificateInfo?.issuerDisplayName
    val title = when {
        !hasCertificate -> stringResource(R.string.smime_signing_dialog_choose_certificate)
        certificateInfo != null -> certificateInfo.subjectDisplayName
        else -> selectedAlias
    }
    val subtitle = when {
        !hasCertificate -> stringResource(R.string.smime_signing_dialog_choose_certificate_subtitle)

        isExpired -> stringResource(R.string.smime_signing_dialog_certificate_expired)

        issuerDisplayName != null -> stringResource(
            R.string.smime_signing_dialog_certificate_issuer,
            issuerDisplayName,
        )

        else -> stringResource(R.string.smime_signing_dialog_certificate_alias_subtitle, selectedAlias)
    }

    CardOutlined(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BoltTheme.spacings.double),
            modifier = Modifier
                .fillMaxWidth()
                .padding(BoltTheme.spacings.double),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(BoltTheme.spacings.half),
                modifier = Modifier.weight(1f),
            ) {
                TextTitleSmall(text = title)
                TextBodySmall(
                    text = subtitle,
                    color = if (isExpired) BoltTheme.colors.error else BoltTheme.colors.onSurfaceVariant,
                )
            }
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun SmimeSigningDialogButtons(
    hasCertificate: Boolean,
    isCertificateExpired: Boolean,
    isSigningEnabledForMessage: Boolean,
    isFirstTimeSendPrompt: Boolean,
    onChooseCertificateClick: () -> Unit,
    onConfirmSignClick: () -> Unit,
    onSendUnsignedClick: () -> Unit,
    onDisableSigningClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(BoltTheme.spacings.default),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            isFirstTimeSendPrompt -> {
                ButtonText(
                    text = stringResource(R.string.smime_signing_dialog_action_send_unsigned),
                    onClick = onSendUnsignedClick,
                )
            }

            isSigningEnabledForMessage -> {
                ButtonText(
                    text = stringResource(R.string.smime_signing_dialog_action_turn_off),
                    onClick = onDisableSigningClick,
                )
            }

            else -> {
                ButtonText(
                    text = stringResource(R.string.smime_signing_dialog_action_cancel),
                    onClick = onDismiss,
                )
            }
        }

        val canConfirm = hasCertificate && !isCertificateExpired
        val primaryText = when {
            !canConfirm -> stringResource(R.string.smime_signing_dialog_action_choose_certificate)
            isFirstTimeSendPrompt -> stringResource(R.string.smime_signing_dialog_action_sign_and_send)
            else -> stringResource(R.string.smime_signing_dialog_action_apply)
        }
        ButtonFilled(
            text = primaryText,
            onClick = if (canConfirm) onConfirmSignClick else onChooseCertificateClick,
        )
    }
}
