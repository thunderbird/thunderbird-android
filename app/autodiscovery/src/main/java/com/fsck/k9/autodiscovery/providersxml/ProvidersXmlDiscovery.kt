package com.fsck.k9.autodiscovery.providersxml


import java.net.URI
import java.net.URISyntaxException

import com.fsck.k9.autodiscovery.ConnectionSettings
import com.fsck.k9.autodiscovery.ConnectionSettingsDiscovery
import com.fsck.k9.backend.BackendManager
import com.fsck.k9.helper.EmailHelper
import com.fsck.k9.helper.UrlEncodingHelper
import org.xmlpull.v1.XmlPullParser
import timber.log.Timber


class ProvidersXmlDiscovery(
        private val backendManager: BackendManager,
        private val xmlProvider: ProvidersXmlProvider
) : ConnectionSettingsDiscovery {

    override fun discover(email: String): ConnectionSettings? {
        val password = ""

        val user = EmailHelper.getLocalPartFromEmailAddress(email) ?: return null
        val domain = EmailHelper.getDomainFromEmailAddress(email) ?: return null

        val provider = findProviderForDomain(domain) ?: return null
        try {
            val userEnc = UrlEncodingHelper.encodeUtf8(user)
            val passwordEnc = UrlEncodingHelper.encodeUtf8(password)

            var incomingUsername = provider.incomingUsernameTemplate!!
            incomingUsername = incomingUsername.replace("\$email", email)
            incomingUsername = incomingUsername.replace("\$user", userEnc)
            incomingUsername = incomingUsername.replace("\$domain", domain)

            val incomingUriTemplate = provider.incomingUriTemplate!!
            val incomingUri = URI(
                    incomingUriTemplate.scheme,
                    "$incomingUsername:$passwordEnc",
                    incomingUriTemplate.host,
                    incomingUriTemplate.port,
                    null,
                    null,
                    null
            )

            var outgoingUsername = provider.outgoingUsernameTemplate
            val outgoingUriTemplate = provider.outgoingUriTemplate!!

            val outgoingUri: URI
            if (outgoingUsername != null) {
                outgoingUsername = outgoingUsername.replace("\$email", email)
                outgoingUsername = outgoingUsername.replace("\$user", userEnc)
                outgoingUsername = outgoingUsername.replace("\$domain", domain)
                outgoingUri = URI(
                        outgoingUriTemplate.scheme,
                        "$outgoingUsername:$passwordEnc",
                        outgoingUriTemplate.host,
                        outgoingUriTemplate.port,
                        null,
                        null,
                        null
                )
            } else {
                outgoingUri = URI(
                        outgoingUriTemplate.scheme,
                        null,
                        outgoingUriTemplate.host,
                        outgoingUriTemplate.port,
                        null,
                        null,
                        null
                )
            }

            val incomingSettings = backendManager.decodeStoreUri(incomingUri.toString())
            val outgoingSettings = backendManager.decodeTransportUri(outgoingUri.toString())
            return ConnectionSettings(incomingSettings, outgoingSettings)
        } catch (use: URISyntaxException) {
            return null
        }
    }

    private fun findProviderForDomain(domain: String): Provider? {
        try {
            val xml = xmlProvider.getXml()
            var provider: Provider? = null

            do {
                val xmlEventType = xml.next()

                if (xmlEventType == XmlPullParser.START_TAG &&
                        "provider" == xml.name &&
                        domain.equals(xml.getAttributeValue(null, "domain"), ignoreCase = true)) {
                    provider = Provider()
                    provider.id = xml.getAttributeValue(null, "id")
                    provider.label = xml.getAttributeValue(null, "label")
                    provider.domain = xml.getAttributeValue(null, "domain")
                } else if (xmlEventType == XmlPullParser.START_TAG &&
                        "incoming" == xml.name &&
                        provider != null) {
                    provider.incomingUriTemplate = URI(xml.getAttributeValue(null, "uri"))
                    provider.incomingUsernameTemplate = xml.getAttributeValue(null, "username")
                } else if (xmlEventType == XmlPullParser.START_TAG &&
                        "outgoing" == xml.name &&
                        provider != null) {
                    provider.outgoingUriTemplate = URI(xml.getAttributeValue(null, "uri"))
                    provider.outgoingUsernameTemplate = xml.getAttributeValue(null, "username")
                } else if (xmlEventType == XmlPullParser.END_TAG &&
                        "provider" == xml.name &&
                        provider != null) {
                    return provider
                }
            } while (xmlEventType != XmlPullParser.END_DOCUMENT)
        } catch (e: Exception) {
            Timber.e(e, "Error while trying to load provider settings.")
        }

        return null
    }


    internal class Provider {
        var id: String? = null
        var label: String? = null
        var domain: String? = null
        var incomingUriTemplate: URI? = null
        var incomingUsernameTemplate: String? = null
        var outgoingUriTemplate: URI? = null
        var outgoingUsernameTemplate: String? = null
    }
}
