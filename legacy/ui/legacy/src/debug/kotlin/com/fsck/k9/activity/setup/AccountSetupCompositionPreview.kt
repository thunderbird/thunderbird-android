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
            senderName = "John Doe",
            senderEmail = "john_doe@gmail.com",
            bccEmail = "john_22@gmail.com",
            saveActionEnabled = false,
            useSignature = true,
            signature = "John",
            signatureLocations = persistentListOf(
                Pair(1, stringResource(R.string.account_settings_signature__location_before_quoted_text)),
                Pair(2, stringResource(R.string.account_settings_signature__location_after_quoted_text)),
            ),
            selectedSignatureLocations = Pair(
                1,
                stringResource(R.string.account_settings_signature__location_before_quoted_text),
            ),
            onEvent = {},
        )
    }
}

@Composable
@PreviewLightDark
internal fun AccountSetupCompositionDontUseSignaturePreview() {
    PreviewWithThemesLightDark {
        AccountSetupCompositionScreen(
            senderName = "John Doe",
            senderEmail = "john_doe@gmail.com",
            bccEmail = "john_22@gmail.com",
            useSignature = false,
            saveActionEnabled = true,
            signature = "John",
            signatureLocations = persistentListOf(
                Pair(1, stringResource(R.string.account_settings_signature__location_before_quoted_text)),
                Pair(2, stringResource(R.string.account_settings_signature__location_after_quoted_text)),
            ),
            selectedSignatureLocations = Pair(
                1,
                stringResource(R.string.account_settings_signature__location_before_quoted_text),
            ),
            onEvent = {},
        )
    }
}
