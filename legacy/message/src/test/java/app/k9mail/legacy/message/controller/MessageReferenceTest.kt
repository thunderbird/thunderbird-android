package app.k9mail.legacy.message.controller

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.test.assertNotNull
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import org.junit.Test

class MessageReferenceTest {

    private val accountId = AccountIdFactory.of("01a0e8d7-87dc-74a4-a52c-3f507c6dc02c")

    @Test
    fun checkIdentityStringFromMessageReference() {
        val messageReference = MessageReference(accountId, 2, "10101010")

        val serialized = messageReference.toIdentityString()

        assertThat(serialized).isEqualTo("#:MDFhMGU4ZDctODdkYy03NGE0LWE1MmMtM2Y1MDdjNmRjMDJj:Mg==:MTAxMDEwMTA=")
    }

    @Test
    fun parseIdentityString() {
        val result = MessageReference.parse("#:MDFhMGU4ZDctODdkYy03NGE0LWE1MmMtM2Y1MDdjNmRjMDJj:Mg==:MTAxMDEwMTA=")

        assertNotNull(result) { messageReference ->
            assertThat(messageReference.accountId).isEqualTo(accountId)
            assertThat(messageReference.folderId).isEqualTo(2)
            assertThat(messageReference.uid).isEqualTo("10101010")
        }
    }

    @Test
    fun parseIdentityStringContainingBadVersionNumber() {
        val messageReference = MessageReference.parse("@:byBoYWkh:MTAxMDEwMTA=")

        assertThat(messageReference).isNull()
    }

    @Test
    fun parseIdentityStringContainingInvalidAccountId() {
        val messageReference = MessageReference.parse("#:bm90LWEtdXVpZA==:Mg==:MTAxMDEwMTA=")

        assertThat(messageReference).isNull()
    }

    @Test
    fun parseNullIdentityString() {
        val messageReference = MessageReference.parse(null)

        assertThat(messageReference).isNull()
    }

    @Test
    fun checkMessageReferenceWithChangedUid() {
        val messageReferenceOne = MessageReference(accountId, 1, "uid")

        val messageReference = messageReferenceOne.withModifiedUid("---")

        assertThat(messageReference.accountId).isEqualTo(accountId)
        assertThat(messageReference.folderId).isEqualTo(1)
        assertThat(messageReference.uid).isEqualTo("---")
    }

    @Test
    fun alternativeEquals() {
        val messageReference = MessageReference(accountId, 1, "uid")

        val equalsResult = messageReference.equals(accountId, 1, "uid")

        assertThat(equalsResult).isTrue()
    }
}
