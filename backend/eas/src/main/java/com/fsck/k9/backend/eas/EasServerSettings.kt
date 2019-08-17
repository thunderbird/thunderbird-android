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
            HttpUrl.Builder()
                    .scheme("http")
                    .host(host)
                    .port(port)
                    .username(encodeUtf8(username))
                    .password(encodeUtf8(password))
                    .build()
                    .toString()
                    .replace("http", when (connectionSecurity) {
                        ConnectionSecurity.NONE -> "eas+"
                        ConnectionSecurity.SSL_TLS_REQUIRED -> "eas+ssl+"
                        else -> throw IllegalArgumentException("Invalid connection security")
                    })
        }

        fun decode(uri: String) =
                HttpUrl.parse(uri
                        .replace("eas+ssl+", "https")
                        .replace("eas+", "http"))!!.run {
                    EasServerSettings(
                            host(),
                            port(),
                            when {
                                scheme().startsWith("https") -> ConnectionSecurity.SSL_TLS_REQUIRED
                                scheme().startsWith("http") -> ConnectionSecurity.NONE
                                else -> throw IllegalArgumentException("Invalid connection scheme")
                            },
                            decodeUtf8(username()),
                            decodeUtf8(password())
                    )
                }
    }
}
