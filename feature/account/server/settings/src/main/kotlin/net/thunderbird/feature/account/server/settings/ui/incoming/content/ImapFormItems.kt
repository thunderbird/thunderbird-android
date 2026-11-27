package net.thunderbird.feature.account.server.settings.ui.incoming.content

import android.content.res.Resources
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.res.stringResource
import app.k9mail.feature.account.common.ui.item.defaultItemPadding
import net.thunderbird.components.ui.bolt.molecule.input.CheckboxInput
import net.thunderbird.components.ui.bolt.molecule.input.TextInput
import net.thunderbird.feature.account.server.settings.R
import net.thunderbird.feature.account.server.settings.ui.common.mapper.toResourceString
import net.thunderbird.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract.Event
import net.thunderbird.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract.State

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
            checked = state.imapSendClientInfo,
            onCheckedChange = { onEvent(Event.ImapSendClientInfoChanged(it)) },
            contentPadding = defaultItemPadding(),
        )
    }
}
