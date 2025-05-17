package com.fsck.k9.backends

import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.store.imap.ImapStoreSettings
import com.fsck.k9.mail.store.imap.ImapStoreSettings.autoDetectNamespace
import com.fsck.k9.mail.store.imap.ImapStoreSettings.pathPrefix
import net.thunderbird.core.android.account.LegacyAccount

fun LegacyAccount.toImapServerSettings(): ServerSettings {
    val serverSettings = incomingServerSettings
    return serverSettings.copy(
        extra = ImapStoreSettings.createExtra(
            autoDetectNamespace = serverSettings.autoDetectNamespace,
            pathPrefix = serverSettings.pathPrefix,
            useCompression = useCompression,
            sendClientInfo = isSendClientInfoEnabled,
        ),
    )
}
