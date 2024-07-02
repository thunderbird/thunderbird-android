package com.fsck.k9.preferences

internal typealias InternalSettingsMap = Map<String, Any?>

interface ValidatedSettings {
    data class Account(
        val uuid: String,
        val name: String?,
        val incoming: Server,
        val outgoing: Server,
        val settings: InternalSettingsMap,
        val identities: List<Identity>,
        val folders: List<Folder>,
    )

    data class Server(
        val type: String,
        val settings: InternalSettingsMap,
        val extras: Map<String, String?>,
    )

    data class Identity(
        val name: String?,
        val email: String,
        val description: String?,
        val settings: InternalSettingsMap,
    )

    data class Folder(
        val name: String,
        val settings: InternalSettingsMap,
    )
}
