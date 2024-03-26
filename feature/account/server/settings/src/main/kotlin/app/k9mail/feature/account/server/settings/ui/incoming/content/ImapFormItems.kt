package app.k9mail.feature.account.server.settings.ui.incoming.content

import android.content.res.Resources
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.designsystem.molecule.input.CheckboxInput
import app.k9mail.core.ui.compose.designsystem.molecule.input.TextInput
import app.k9mail.feature.account.common.ui.item.defaultItemPadding
import app.k9mail.feature.account.server.settings.R
import app.k9mail.feature.account.server.settings.ui.common.mapper.toResourceString
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract.Event
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract.State

internal fun LazyListScope.imapFormItems(
    state: State,
    onEvent: (Event) -> Unit,
    resources: Resources,
) {
    item {
        CheckboxInput(
            text = stringResource(id = R.string.account_server_settings_incoming_imap_namespace_label),
            checked = state.imapAutodetectNamespaceEnabled,
            onCheckedChange = { onEvent(Event.ImapAutoDetectNamespaceChanged(it)) },
            contentPadding = defaultItemPadding(),
        )
    }

    item {
        if (state.imapAutodetectNamespaceEnabled) {
            TextInput(
                onTextChange = {},
                label = stringResource(id = R.string.account_server_settings_incoming_imap_prefix_label),
                contentPadding = defaultItemPadding(),
                isEnabled = false,
            )
        } else {
            TextInput(
                text = state.imapPrefix.value,
                errorMessage = state.imapPrefix.error?.toResourceString(resources),
                onTextChange = { onEvent(Event.ImapPrefixChanged(it)) },
                label = stringResource(id = R.string.account_server_settings_incoming_imap_prefix_label),
                contentPadding = defaultItemPadding(),
            )
        }
    }

    item {
        CheckboxInput(
            text = stringResource(id = R.string.account_server_settings_incoming_imap_compression_label),
            checked = state.imapUseCompression,
            onCheckedChange = { onEvent(Event.ImapUseCompressionChanged(it)) },
            contentPadding = defaultItemPadding(),
        )
    }

    item {
        CheckboxInput(
            text = stringResource(R.string.account_server_settings_incoming_imap_send_client_info_label),
            checked = state.imapSendClientId,
            onCheckedChange = { onEvent(Event.ImapSendClientIdChanged(it)) },
            contentPadding = defaultItemPadding(),
        )
    }
}
