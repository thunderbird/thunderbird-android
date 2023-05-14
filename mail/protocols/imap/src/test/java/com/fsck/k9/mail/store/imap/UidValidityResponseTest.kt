package com.fsck.k9.mail.store.imap

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.prop
import org.junit.Test

class UidValidityResponseTest {
    @Test
    fun validResponseWithText() {
        val response = ImapResponseHelper.createImapResponse("* OK [UIDVALIDITY 23] UIDs valid")

        val result = UidValidityResponse.parse(response)

        assertThat(result).isNotNull()
            .prop(UidValidityResponse::uidValidity).isEqualTo(23)
    }

    @Test
    fun validResponseWithoutText() {
        val response = ImapResponseHelper.createImapResponse("* OK [UIDVALIDITY 42]")

        val result = UidValidityResponse.parse(response)

        assertThat(result).isNotNull()
            .prop(UidValidityResponse::uidValidity).isEqualTo(42)
    }

    @Test
    fun taggedResponse_shouldReturnNull() {
        assertNotValid("99 OK [UIDVALIDITY 42]")
    }

    @Test
    fun noResponse_shouldReturnNull() {
        assertNotValid("* NO [UIDVALIDITY 42]")
    }

    @Test
    fun responseTextWithOnlyOneItem_shouldReturnNull() {
        assertNotValid("* OK [UIDVALIDITY]")
    }

    @Test
    fun uidValidityIsNotANumber_shouldReturnNull() {
        assertNotValid("* OK [UIDVALIDITY fourtytwo]")
    }

    @Test
    fun negativeUidValidity_shouldReturnNull() {
        assertNotValid("* OK [UIDVALIDITY -1]")
    }

    @Test
    fun uidValidityOutsideRange_shouldReturnNull() {
        assertNotValid("* OK [UIDVALIDITY 4294967296]")
    }

    private fun assertNotValid(response: String) {
        val result = UidValidityResponse.parse(ImapResponseHelper.createImapResponse(response))

        assertThat(result).isNull()
    }
}
