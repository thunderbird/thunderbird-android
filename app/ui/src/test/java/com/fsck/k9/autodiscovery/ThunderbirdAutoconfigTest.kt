package com.fsck.k9.autodiscovery

import com.fsck.k9.RobolectricTest
import junit.framework.Assert.*
import org.junit.Test

class ThunderbirdAutoconfigTest : RobolectricTest() {
    private val parser = ThunderbirdAutoconfigParser()

    @Test
    fun settingsExtract() {
        val settings = parser.parseSettings("""<?xml version="1.0"?>
<clientConfig version="1.1">
    <emailProvider id="metacode.biz">
      <domain>metacode.biz</domain>

      <incomingServer type="imap">
         <hostname>imap.googlemail.com</hostname>
         <port>993</port>
         <socketType>SSL</socketType>
         <username>%EMAILADDRESS%</username>
         <authentication>OAuth2</authentication>
         <authentication>password-cleartext</authentication>
      </incomingServer>

      <outgoingServer type="smtp">
         <hostname>smtp.googlemail.com</hostname>
         <port>465</port>
         <socketType>SSL</socketType>
         <username>%EMAILADDRESS%</username>
         <authentication>OAuth2</authentication>
         <authentication>password-cleartext</authentication>
         <addThisServer>true</addThisServer>
      </outgoingServer>
    </emailProvider>

</clientConfig>""".byteInputStream(), "test@metacode.biz")

        assertNotNull(settings)
        assertNotNull(settings?.outgoing)
        assertNotNull(settings?.incoming)

        assertEquals("imap.googlemail.com", settings?.incoming?.host)
        assertEquals(993, settings?.incoming?.port)
        assertEquals("test@metacode.biz", settings?.incoming?.username)

        assertEquals("smtp.googlemail.com", settings?.outgoing?.host)
        assertEquals(465, settings?.outgoing?.port)
        assertEquals("test@metacode.biz", settings?.outgoing?.username)
    }

    @Test
    fun pickFirstServer() {
        val settings = parser.parseSettings("""<?xml version="1.0"?>
<clientConfig version="1.1">
    <emailProvider id="metacode.biz">
      <domain>metacode.biz</domain>

      <incomingServer type="imap">
         <hostname>imap.googlemail.com</hostname>
         <port>993</port>
         <socketType>SSL</socketType>
         <username>%EMAILADDRESS%</username>
         <authentication>OAuth2</authentication>
         <authentication>password-cleartext</authentication>
      </incomingServer>

      <outgoingServer type="smtp">
         <hostname>first</hostname>
         <port>465</port>
         <socketType>SSL</socketType>
         <username>%EMAILADDRESS%</username>
         <authentication>OAuth2</authentication>
         <authentication>password-cleartext</authentication>
         <addThisServer>true</addThisServer>
      </outgoingServer>

      <outgoingServer type="smtp">
         <hostname>second</hostname>
         <port>465</port>
         <socketType>SSL</socketType>
         <username>%EMAILADDRESS%</username>
         <authentication>OAuth2</authentication>
         <authentication>password-cleartext</authentication>
         <addThisServer>true</addThisServer>
      </outgoingServer>
    </emailProvider>

</clientConfig>""".byteInputStream(), "test@metacode.biz")

        assertNotNull(settings)
        assertNotNull(settings?.outgoing)
        assertNotNull(settings?.incoming)

        assertEquals("first", settings?.outgoing?.host)
        assertEquals(465, settings?.outgoing?.port)
        assertEquals("test@metacode.biz", settings?.outgoing?.username)
    }

    @Test
    fun invalidResponse() {
        val settings = parser.parseSettings("""<?xml version="1.0"?>
<clientConfig version="1.1">
    <emailProvider id="metacode.biz">
      <domain>metacode.biz</domain>""".byteInputStream(), "test@metacode.biz")

        assertNull(settings)
    }

    @Test
    fun notCompleteConfiguration() {
        val settings = parser.parseSettings("""<?xml version="1.0"?>
<clientConfig version="1.1">
    <emailProvider id="metacode.biz">
      <domain>metacode.biz</domain>

      <incomingServer type="imap">
         <hostname>imap.googlemail.com</hostname>
         <port>993</port>
         <socketType>SSL</socketType>
         <username>%EMAILADDRESS%</username>
         <authentication>OAuth2</authentication>
         <authentication>password-cleartext</authentication>
      </incomingServer>
    </emailProvider>
</clientConfig>""".byteInputStream(), "test@metacode.biz")

        assertNull(settings)
    }

    @Test
    fun generatedUrls() {
        val email = "test@metacode.biz"

        val actual = ThunderbirdAutoconfigFetcher.getAutodiscoveryAddress(email)

        val expected = "https://metacode.biz/.well-known/autoconfig/mail/config-v1.1.xml?emailaddress=test%40metacode.biz"

        assertEquals(expected, actual)
    }
}
