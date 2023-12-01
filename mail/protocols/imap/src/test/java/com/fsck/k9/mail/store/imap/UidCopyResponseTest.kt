package com.fsck.k9.mail.store.imap

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.fsck.k9.mail.store.imap.ImapResponseHelper.createImapResponseList
import kotlin.test.Test

class UidCopyResponseTest {
    @Test
    fun `parse() with COPYUID response should return UID mapping`() {
        val imapResponses = createImapResponseList("x OK [COPYUID 1 1,3:5 7:10] Success")

        val result = UidCopyResponse.parse(imapResponses)

        assertThat(result).isNotNull()
            .transform { it.uidMapping }
            .isEqualTo(
                mapOf(
                    "1" to "7",
                    "3" to "8",
                    "4" to "9",
                    "5" to "10",
                ),
            )
    }

    @Test
    fun `parse() with allowed untagged COPYUID response should return UID mapping`() {
        val imapResponses = createImapResponseList(
            "* OK [COPYUID 1 1,3 10:11]",
            "* 1 EXPUNGE",
            "* 1 EXPUNGE",
            "x OK MOVE completed",
        )

        val result = UidCopyResponse.parse(imapResponses, allowUntaggedResponse = true)

        assertThat(result).isNotNull()
            .transform { it.uidMapping }
            .isEqualTo(
                mapOf(
                    "1" to "10",
                    "3" to "11",
                ),
            )
    }

    @Test
    fun `parse() with untagged response when not allowed should return null`() {
        val imapResponses = createImapResponseList("* OK [COPYUID 1 1,3:5 7:10] Success")

        val result = UidCopyResponse.parse(imapResponses)

        assertThat(result).isNull()
    }

    @Test
    fun `parse() with response containing too few items should return null`() {
        val imapResponses = createImapResponseList("x OK")

        val result = UidCopyResponse.parse(imapResponses)

        assertThat(result).isNull()
    }

    @Test
    fun `parse() without OK response should return null`() {
        val imapResponses = createImapResponseList("x BYE Logout")

        val result = UidCopyResponse.parse(imapResponses)

        assertThat(result).isNull()
    }

    @Test
    fun `parse() without response text list should return null`() {
        val imapResponses = createImapResponseList("x OK Success")

        val result = UidCopyResponse.parse(imapResponses)

        assertThat(result).isNull()
    }

    @Test
    fun `parse() with response text list containing too few items should return null`() {
        val imapResponses = createImapResponseList("x OK [A B C] Success")
        val result = UidCopyResponse.parse(imapResponses)
        assertThat(result).isNull()
    }

    @Test
    fun `parse() without COPYUID response should return null`() {
        val imapResponses = createImapResponseList("x OK [A B C D] Success")

        val result = UidCopyResponse.parse(imapResponses)

        assertThat(result).isNull()
    }

    @Test
    fun `parse() with first COPYUID argument not being a string should return null`() {
        val imapResponses = createImapResponseList("x OK [COPYUID () C D] Success")

        val result = UidCopyResponse.parse(imapResponses)

        assertThat(result).isNull()
    }

    @Test
    fun `parse() with second COPYUID argument not being a string should return null`() {
        val imapResponses = createImapResponseList("x OK [COPYUID B () D] Success")

        val result = UidCopyResponse.parse(imapResponses)

        assertThat(result).isNull()
    }

    @Test
    fun `parse() with third COPYUID argument not being a string should return null`() {
        val imapResponses = createImapResponseList("x OK [COPYUID B C ()] Success")

        val result = UidCopyResponse.parse(imapResponses)

        assertThat(result).isNull()
    }

    @Test
    fun `parse() with non-number COPYUID arguments should return null`() {
        val imapResponses = createImapResponseList("x OK [COPYUID B C D] Success")

        val result = UidCopyResponse.parse(imapResponses)

        assertThat(result).isNull()
    }

    @Test
    fun `parse() with unbalanced COPYUID arguments should return null`() {
        val imapResponses = createImapResponseList("x OK [COPYUID B 1 1,2] Success")

        val result = UidCopyResponse.parse(imapResponses)

        assertThat(result).isNull()
    }
}
