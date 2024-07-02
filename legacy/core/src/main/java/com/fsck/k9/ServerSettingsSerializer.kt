package com.fsck.k9

import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonReader.Token
import com.squareup.moshi.JsonWriter

class ServerSettingsSerializer {
    private val adapter = ServerSettingsAdapter()

    fun serialize(serverSettings: ServerSettings): String {
        return adapter.toJson(serverSettings)
    }

    fun deserialize(json: String): ServerSettings {
        return adapter.fromJson(json)!!
    }
}

private const val KEY_TYPE = "type"
private const val KEY_HOST = "host"
private const val KEY_PORT = "port"
private const val KEY_CONNECTION_SECURITY = "connectionSecurity"
private const val KEY_AUTHENTICATION_TYPE = "authenticationType"
private const val KEY_USERNAME = "username"
private const val KEY_PASSWORD = "password"
private const val KEY_CLIENT_CERTIFICATE_ALIAS = "clientCertificateAlias"

private val JSON_KEYS = JsonReader.Options.of(
    KEY_TYPE,
    KEY_HOST,
    KEY_PORT,
    KEY_CONNECTION_SECURITY,
    KEY_AUTHENTICATION_TYPE,
    KEY_USERNAME,
    KEY_PASSWORD,
    KEY_CLIENT_CERTIFICATE_ALIAS,
)

private class ServerSettingsAdapter : JsonAdapter<ServerSettings>() {
    override fun fromJson(reader: JsonReader): ServerSettings {
        reader.beginObject()

        var type: String? = null
        var host: String? = null
        var port: Int? = null
        var connectionSecurity: ConnectionSecurity? = null
        var authenticationType: AuthType? = null
        var username: String? = null
        var password: String? = null
        var clientCertificateAlias: String? = null
        val extra = mutableMapOf<String, String?>()

        while (reader.hasNext()) {
            when (reader.selectName(JSON_KEYS)) {
                0 -> type = reader.nextString()
                1 -> host = reader.nextString()
                2 -> port = reader.nextInt()
                3 -> connectionSecurity = ConnectionSecurity.valueOf(reader.nextString())
                4 -> authenticationType = AuthType.valueOf(reader.nextString())
                5 -> username = reader.nextString()
                6 -> password = reader.nextStringOrNull()
                7 -> clientCertificateAlias = reader.nextStringOrNull()
                else -> {
                    val key = reader.nextName()
                    val value = reader.nextStringOrNull()
                    extra[key] = value
                }
            }
        }

        reader.endObject()

        requireNotNull(type) { "'type' must not be missing" }
        requireNotNull(host) { "'host' must not be missing" }
        requireNotNull(port) { "'port' must not be missing" }
        requireNotNull(connectionSecurity) { "'connectionSecurity' must not be missing" }
        requireNotNull(authenticationType) { "'authenticationType' must not be missing" }
        requireNotNull(username) { "'username' must not be missing" }

        return ServerSettings(
            type,
            host,
            port,
            connectionSecurity,
            authenticationType,
            username,
            password,
            clientCertificateAlias,
            extra,
        )
    }

    override fun toJson(writer: JsonWriter, serverSettings: ServerSettings?) {
        requireNotNull(serverSettings)

        writer.beginObject()
        writer.serializeNulls = true

        writer.name(KEY_TYPE).value(serverSettings.type)
        writer.name(KEY_HOST).value(serverSettings.host)
        writer.name(KEY_PORT).value(serverSettings.port)
        writer.name(KEY_CONNECTION_SECURITY).value(serverSettings.connectionSecurity.name)
        writer.name(KEY_AUTHENTICATION_TYPE).value(serverSettings.authenticationType.name)
        writer.name(KEY_USERNAME).value(serverSettings.username)
        writer.name(KEY_PASSWORD).value(serverSettings.password)
        writer.name(KEY_CLIENT_CERTIFICATE_ALIAS).value(serverSettings.clientCertificateAlias)

        for ((key, value) in serverSettings.extra) {
            writer.name(key).value(value)
        }

        writer.endObject()
    }

    private fun JsonReader.nextStringOrNull(): String? {
        return if (peek() == Token.NULL) nextNull() else nextString()
    }
}
