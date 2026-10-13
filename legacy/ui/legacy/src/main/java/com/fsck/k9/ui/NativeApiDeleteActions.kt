package com.fsck.k9.ui

import net.thunderbird.core.common.mail.Protocols
import net.thunderbird.core.common.mail.ZohoMailServerSettings

internal fun isPermanentDeleteFolder(
    protocol: String,
    trashFolderId: Long?,
    spamFolderId: Long?,
    folderId: Long,
): Boolean = when (protocol) {
    Protocols.GMAIL_API -> trashFolderId == folderId
    Protocols.ZOHO_MAIL_API -> spamFolderId == folderId
    else -> false
}

internal fun isZohoMailApiProtocol(protocol: String): Boolean = protocol == Protocols.ZOHO_MAIL_API

object NativeApiAccountCapabilities {
    @JvmStatic
    fun isZohoIncomingOnly(protocol: String, extra: Map<String, String?>): Boolean =
        protocol == Protocols.ZOHO_MAIL_API && extra[ZohoMailServerSettings.EXTRA_INCOMING_ONLY].toBoolean()
}
