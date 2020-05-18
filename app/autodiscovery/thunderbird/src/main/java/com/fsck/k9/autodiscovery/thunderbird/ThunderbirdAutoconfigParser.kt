package com.fsck.k9.autodiscovery.thunderbird

import com.fsck.k9.autodiscovery.api.DiscoveredServerSettings
import com.fsck.k9.autodiscovery.api.DiscoveryResults
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
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
    fun parseSettings(stream: InputStream, email: String): DiscoveryResults? {
        val factory = XmlPullParserFactory.newInstance()
        val xpp = factory.newPullParser()

        xpp.setInput(InputStreamReader(stream))

        val incomingServers = mutableListOf<DiscoveredServerSettings>()
        val outgoingServers = mutableListOf<DiscoveredServerSettings>()
        var eventType = xpp.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                when (xpp.name) {
                    "incomingServer" -> {
                        incomingServers += parseServer(xpp, "incomingServer", email)
                    }
                    "outgoingServer" -> {
                        outgoingServers += parseServer(xpp, "outgoingServer", email)
                    }
                }
            }
            eventType = xpp.next()
        }
        return DiscoveryResults(incomingServers, outgoingServers)
    }

    private fun parseServer(xpp: XmlPullParser, nodeName: String, email: String): DiscoveredServerSettings {
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

        return DiscoveredServerSettings(type, host!!, port!!, connectionSecurity!!, authType, username)
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
