package app.k9mail.legacy.message.controller

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.test.assertNotNull
import org.junit.Test

class MessageReferenceTest {
    @Test
    fun checkIdentityStringFromMessageReference() {
        val messageReference = MessageReference("o hai!", 2, "10101010")

        val serialized = messageReference.toIdentityString()

        assertThat(serialized).isEqualTo("#:byBoYWkh:Mg==:MTAxMDEwMTA=")
    }

    @Test
    fun parseIdentityString() {
        val result = MessageReference.parse("#:byBoYWkh:Mg==:MTAxMDEwMTA=")

        assertNotNull(result) { messageReference ->
            assertThat(messageReference.accountUuid).isEqualTo("o hai!")
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
    fun parseNullIdentityString() {
        val messageReference = MessageReference.parse(null)

        assertThat(messageReference).isNull()
    }

    @Test
    fun checkMessageReferenceWithChangedUid() {
        val messageReferenceOne = MessageReference("account", 1, "uid")

        val messageReference = messageReferenceOne.withModifiedUid("---")

        assertThat(messageReference.accountUuid).isEqualTo("account")
        assertThat(messageReference.folderId).isEqualTo(1)
        assertThat(messageReference.uid).isEqualTo("---")
    }

    @Test
    fun alternativeEquals() {
        val messageReference = MessageReference("account", 1, "uid")

        val equalsResult = messageReference.equals("account", 1, "uid")

        assertThat(equalsResult).isTrue()
    }
}
