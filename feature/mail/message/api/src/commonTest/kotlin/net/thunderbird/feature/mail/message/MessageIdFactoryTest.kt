package net.thunderbird.feature.mail.message

import assertk.Assert
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class MessageIdFactoryTest {

    @Test
    fun `given existing UUID when of is called then returns compatible MessageId`() {
        // Arrange
        val raw = "123e4567-e89b-42d3-a456-426655440000"

        // Act
        val messageId = MessageIdFactory.of(raw)

        // Assert
        assertThat(messageId.value).isEqualTo(Uuid.parse(raw))
    }

    @Test
    fun `given create is called then returns UUIDv7 MessageId`() {
        // Arrange + Act
        val messageId = MessageIdFactory.create()

        // Assert
        assertThat(messageId.value).isUuidV7()
    }

    private fun Assert<Uuid>.isUuidV7() = given { actual ->
        actual.toLongs { mostSignificantBits, leastSignificantBits ->
            assertThat((mostSignificantBits ushr 12) and 0x0F).isEqualTo(7L)
            assertThat(leastSignificantBits ushr 62).isEqualTo(2L)
        }
    }
}
