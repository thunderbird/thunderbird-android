package com.fsck.k9.backend.eas

import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.helper.UrlEncodingHelper.decodeUtf8
import com.fsck.k9.mail.helper.UrlEncodingHelper.encodeUtf8
import okhttp3.HttpUrl

public class EasServerSettings(
        host: String,
        port: Int,
        connectionSecurity: ConnectionSecurity,
        username: String,
        password: String
) : ServerSettings("eas",
        host, port,
        connectionSecurity,
        null,
        username,
        password,
        null) {

    companion object {
        fun encode(settings: ServerSettings) = settings.run {
            val url = HttpUrl.Builder()
                    .scheme(when (connectionSecurity) {
                        ConnectionSecurity.NONE -> "http"
                        ConnectionSecurity.SSL_TLS_REQUIRED -> "https"
                        else -> throw IllegalArgumentException("Invalid connection security")
                    })
                    .host(host)
                    .port(port)
                    .username(encodeUtf8(username))
                    .password(encodeUtf8(password))
                    .build()
                    .toString()

            return@run "eas+$url"
        }

        fun decode(uri: String): EasServerSettings {
            val url = uri.substringAfter("eas+")

            return HttpUrl.parse(url)?.run {
                EasServerSettings(
                        host(),
                        port(),
                        when (scheme()) {
                            "https" -> ConnectionSecurity.SSL_TLS_REQUIRED
                            "http" -> ConnectionSecurity.NONE
                            else -> throw IllegalArgumentException("Invalid connection scheme")
                        },
                        decodeUtf8(username()),
                        decodeUtf8(password())
                )
            } ?: throw IllegalArgumentException()
        }
    }
}
