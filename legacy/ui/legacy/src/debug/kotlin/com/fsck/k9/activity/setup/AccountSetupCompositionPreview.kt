package com.fsck.k9.activity.setup

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemesLightDark
import com.fsck.k9.ui.R
import kotlinx.collections.immutable.persistentListOf

@Composable
@PreviewLightDark
internal fun AccountSetupCompositionUseSignaturePreview() {
    PreviewWithThemesLightDark {
        AccountSetupCompositionScreen(
            senderName = "Shamim Emon",
            senderEmail = "emon9891",
            bccEmail = "emon2423@gmail.com",
            saveActionEnabled = false,
            useSignature = true,
            signature = "Emon",
            signatureLocations = persistentListOf(
                Pair(1, stringResource(R.string.account_settings_signature__location_before_quoted_text)),
                Pair(2, stringResource(R.string.account_settings_signature__location_after_quoted_text)),
            ),
            selectedSignatureLocations = Pair(
                1,
                stringResource(R.string.account_settings_signature__location_before_quoted_text),
            ),
            onSignatureLocationChange = {},
            onUseSignatureChange = {},
            onSignatureChange = {},
            onBackPressed = {},
            onSavePressed = {},
            onSenderNameChange = {},
            onSenderEmailChange = {},
            onBccEmailChange = {},
        )
    }
}

@Composable
@PreviewLightDark
internal fun AccountSetupCompositionDontUseSignaturePreview() {
    PreviewWithThemesLightDark {
        AccountSetupCompositionScreen(
            senderName = "Shamim Emon",
            senderEmail = "emon9891@gmail.com",
            bccEmail = "emon2423@gmail.com",
            useSignature = false,
            saveActionEnabled = true,
            signature = "Emon",
            signatureLocations = persistentListOf(
                Pair(1, stringResource(R.string.account_settings_signature__location_before_quoted_text)),
                Pair(2, stringResource(R.string.account_settings_signature__location_after_quoted_text)),
            ),
            selectedSignatureLocations = Pair(
                1,
                stringResource(R.string.account_settings_signature__location_before_quoted_text),
            ),
            onSignatureLocationChange = {},
            onUseSignatureChange = {},
            onSignatureChange = {},
            onBackPressed = {},
            onSavePressed = {},
            onSenderNameChange = {},
            onSenderEmailChange = {},
            onBccEmailChange = {},
        )
    }
}
