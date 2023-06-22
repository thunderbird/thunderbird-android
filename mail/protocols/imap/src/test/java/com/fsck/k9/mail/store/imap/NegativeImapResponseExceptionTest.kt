package com.fsck.k9.mail.store.imap

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.fsck.k9.mail.store.imap.ImapResponseHelper.createImapResponseList
import kotlin.test.Test

class NegativeImapResponseExceptionTest {
    @Test
    fun `responseText property should contain response text of last response`() {
        val exception = NegativeImapResponseException(
            message = "Test",
            responses = createImapResponseList("x NO [AUTHENTICATIONFAILED] Authentication error #23"),
        )

        assertThat(exception.responseText).isEqualTo("Authentication error #23")
    }

    @Test
    fun `responseText property should be null when last response does not contain response text`() {
        val exception = NegativeImapResponseException(
            message = "Test",
            responses = createImapResponseList("x NO"),
        )

        assertThat(exception.responseText).isNull()
    }

    @Test
    fun `alertText property should contain alert text of last response`() {
        val exception = NegativeImapResponseException(
            message = "Test",
            responses = createImapResponseList("x NO [ALERT] Service is shutting down"),
        )

        assertThat(exception.alertText).isEqualTo("Service is shutting down")
    }

    @Test
    fun `alertText property should be null when last response does not contain alert text`() {
        val exception = NegativeImapResponseException(
            message = "Test",
            responses = createImapResponseList("x NO Not allowed"),
        )

        assertThat(exception.alertText).isNull()
    }

    @Test
    fun `wasByeResponseReceived() should return true when BYE response was received`() {
        val exception = NegativeImapResponseException(
            message = "Test",
            responses = createImapResponseList(
                "* EXISTS 1",
                "* BYE",
                "x OK",
            ),
        )

        assertThat(exception.wasByeResponseReceived()).isTrue()
    }

    @Test
    fun `wasByeResponseReceived() should return false when no BYE response was received`() {
        val exception = NegativeImapResponseException(
            message = "Test",
            responses = createImapResponseList(
                "* EXISTS 1",
                "x OK",
            ),
        )

        assertThat(exception.wasByeResponseReceived()).isFalse()
    }
}
