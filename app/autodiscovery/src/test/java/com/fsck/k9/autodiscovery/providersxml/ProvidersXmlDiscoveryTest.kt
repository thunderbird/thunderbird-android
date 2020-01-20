package com.fsck.k9.autodiscovery.providersxml

import com.fsck.k9.RobolectricTest
import com.fsck.k9.backend.BackendManager
import com.fsck.k9.backend.imap.ImapStoreUriDecoder
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.transport.smtp.SmtpTransportUriDecoder
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.mock
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.robolectric.RuntimeEnvironment

class ProvidersXmlDiscoveryTest : RobolectricTest() {
    val backendManager = mock<BackendManager> {
        on { decodeStoreUri(anyString()) } doAnswer { mock -> ImapStoreUriDecoder.decode(mock.getArgument(0)) }
        on { decodeTransportUri(anyString()) } doAnswer { mock ->
            SmtpTransportUriDecoder.decodeSmtpUri(mock.getArgument(0))
        }
    }
    val xmlProvider = ProvidersXmlProvider(RuntimeEnvironment.application)
    val providersXmlDiscovery = ProvidersXmlDiscovery(backendManager, xmlProvider)

    @Test
    fun discover_withGmailDomain_shouldReturnCorrectSettings() {
        val connectionSettings = providersXmlDiscovery.discover("user@gmail.com")

        assertThat(connectionSettings).isNotNull()
        with(connectionSettings!!.incoming) {
            assertThat(host).isEqualTo("imap.gmail.com")
            assertThat(connectionSecurity).isEqualTo(ConnectionSecurity.SSL_TLS_REQUIRED)
            assertThat(authenticationType).isEqualTo(AuthType.PLAIN)
            assertThat(username).isEqualTo("user@gmail.com")
        }
        with(connectionSettings.outgoing) {
            assertThat(host).isEqualTo("smtp.gmail.com")
            assertThat(connectionSecurity).isEqualTo(ConnectionSecurity.SSL_TLS_REQUIRED)
            assertThat(authenticationType).isEqualTo(AuthType.PLAIN)
            assertThat(username).isEqualTo("user@gmail.com")
        }
    }

    @Test
    fun discover_withUnknownDomain_shouldReturnNull() {
        val connectionSettings = providersXmlDiscovery.discover("user@not.present.in.providers.xml.example")

        assertThat(connectionSettings).isNull()
    }
}
