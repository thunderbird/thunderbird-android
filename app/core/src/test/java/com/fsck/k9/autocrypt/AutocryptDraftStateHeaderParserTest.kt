package com.fsck.k9.autocrypt

import com.fsck.k9.RobolectricTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AutocryptDraftStateHeaderParserTest : RobolectricTest() {
    internal var autocryptHeaderParser = AutocryptDraftStateHeaderParser()

    @Test
    fun testEncryptReplyByChoice() {
        val draftStateHeader = AutocryptDraftStateHeader(true, false, true, true, false)

        val parsedHeader = autocryptHeaderParser.parseAutocryptDraftStateHeader(draftStateHeader.toHeaderValue())

        assertThat(parsedHeader).isEqualTo(draftStateHeader)
    }

    @Test
    fun testSignOnly() {
        val parsedHeader = autocryptHeaderParser.parseAutocryptDraftStateHeader("encrypt=no; _by-choice=yes; _sign-only=yes")

        with(parsedHeader!!) {
            assertThat(isEncrypt).isFalse()
            assertThat(isByChoice).isTrue()
            assertThat(isSignOnly).isTrue()
            assertThat(isPgpInline).isFalse()
            assertThat(isReply).isFalse()
        }
    }

    @Test
    fun badCritical() {
        val parsedHeader = autocryptHeaderParser.parseAutocryptDraftStateHeader("encrypt=no; badcritical=value")

        assertThat(parsedHeader).isNull()
    }

    @Test
    fun missingEncrypt() {
        val parsedHeader = autocryptHeaderParser.parseAutocryptDraftStateHeader("encrpt-with-typo=no; _non_critical=value")

        assertThat(parsedHeader).isNull()
    }

    @Test
    fun unknownNonCritical() {
        val parsedHeader = autocryptHeaderParser.parseAutocryptDraftStateHeader("encrypt=no; _non-critical=value")

        assertThat(parsedHeader).isNotNull()
    }
}
