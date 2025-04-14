package com.fsck.k9.autocrypt

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import net.thunderbird.core.android.testing.RobolectricTest
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
        val parsedHeader =
            autocryptHeaderParser.parseAutocryptDraftStateHeader("encrypt=no; _by-choice=yes; _sign-only=yes")

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
        val parsedHeader =
            autocryptHeaderParser.parseAutocryptDraftStateHeader("encrpt-with-typo=no; _non_critical=value")

        assertThat(parsedHeader).isNull()
    }

    @Test
    fun unknownNonCritical() {
        val parsedHeader = autocryptHeaderParser.parseAutocryptDraftStateHeader("encrypt=no; _non-critical=value")

        assertThat(parsedHeader).isNotNull()
    }
}
