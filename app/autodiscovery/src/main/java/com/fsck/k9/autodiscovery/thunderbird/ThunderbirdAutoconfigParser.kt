package com.fsck.k9.autodiscovery.thunderbird

import com.fsck.k9.autodiscovery.ConnectionSettings
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory

/**
 * Parser for Thunderbird's
 * [Autoconfig file format](https://wiki.mozilla.org/Thunderbird:Autoconfiguration:ConfigFileFormat)
 */
class ThunderbirdAutoconfigParser {
    fun parseSettings(stream: InputStream, email: String): ConnectionSettings? {
        var incoming: ServerSettings? = null
        var outgoing: ServerSettings? = null

        val factory = XmlPullParserFactory.newInstance()
        val xpp = factory.newPullParser()

        xpp.setInput(InputStreamReader(stream))

        var eventType = xpp.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                when (xpp.name) {
                    "incomingServer" -> {
                        incoming = parseServer(xpp, "incomingServer", email)
                    }
                    "outgoingServer" -> {
                        outgoing = parseServer(xpp, "outgoingServer", email)
                    }
                }

                if (incoming != null && outgoing != null) {
                    return ConnectionSettings(incoming, outgoing)
                }
            }
            eventType = xpp.next()
        }
        return null
    }

    private fun parseServer(xpp: XmlPullParser, nodeName: String, email: String): ServerSettings {
        val type = xpp.getAttributeValue(null, "type")
        var host: String? = null
        var username: String? = null
        var port: Int? = null
        var authType: AuthType? = null
        var connectionSecurity: ConnectionSecurity? = null

        var eventType = xpp.eventType
        while (!(eventType == XmlPullParser.END_TAG && nodeName == xpp.name)) {
            if (eventType == XmlPullParser.START_TAG) {
                when (xpp.name) {
                    "hostname" -> {
                        host = getText(xpp)
                    }
                    "port" -> {
                        port = getText(xpp).toInt()
                    }
                    "username" -> {
                        username = getText(xpp).replace("%EMAILADDRESS%", email)
                    }
                    "authentication" -> {
                        if (authType == null) authType = parseAuthType(getText(xpp))
                    }
                    "socketType" -> {
                        connectionSecurity = parseSocketType(getText(xpp))
                    }
                }
            }
            eventType = xpp.next()
        }

        return ServerSettings(type, host, port!!, connectionSecurity, authType, username, null, null)
    }

    private fun parseAuthType(authentication: String): AuthType? {
        return when (authentication) {
            "password-cleartext" -> AuthType.PLAIN
            "TLS-client-cert" -> AuthType.EXTERNAL
            "secure" -> AuthType.CRAM_MD5
            else -> null
        }
    }

    private fun parseSocketType(socketType: String): ConnectionSecurity? {
        return when (socketType) {
            "plain" -> ConnectionSecurity.NONE
            "SSL" -> ConnectionSecurity.SSL_TLS_REQUIRED
            "STARTTLS" -> ConnectionSecurity.STARTTLS_REQUIRED
            else -> null
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun getText(xpp: XmlPullParser): String {
        val eventType = xpp.next()
        return if (eventType != XmlPullParser.TEXT) "" else xpp.text
    }
}
