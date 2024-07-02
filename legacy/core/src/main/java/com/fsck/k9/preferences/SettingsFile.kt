package com.fsck.k9.preferences

internal typealias SettingsMap = Map<String, String>

internal interface SettingsFile {
    data class Contents(
        val contentVersion: Int,
        val globalSettings: SettingsMap?,
        val accounts: List<Account>,
    )

    data class Account(
        val uuid: String,
        val name: String?,
        val incoming: Server?,
        val outgoing: Server?,
        val settings: SettingsMap?,
        val identities: List<Identity>?,
        val folders: List<Folder>?,
    )

    data class Server(
        val type: String?,
        val host: String?,
        val port: String?,
        val connectionSecurity: String?,
        val authenticationType: String?,
        val username: String?,
        val password: String?,
        val clientCertificateAlias: String?,
        val extras: SettingsMap?,
    )

    data class Identity(
        val name: String?,
        val email: String?,
        val description: String?,
        val settings: SettingsMap?,
    )

    data class Folder(
        val name: String?,
        val settings: SettingsMap?,
    )
}
