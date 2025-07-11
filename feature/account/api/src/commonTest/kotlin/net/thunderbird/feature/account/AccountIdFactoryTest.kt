package net.thunderbird.feature.account

import assertk.Assert
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEqualTo
import kotlin.test.Test
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class AccountIdFactoryTest {

    @Test
    fun `create should return AccountId with the same id`() {
        val id = "123e4567-e89b-12d3-a456-426614174000"

        val result = AccountIdFactory.of(id)

        assertThat(result.asRaw()).isEqualTo(id)
    }

    @Test
    fun `create should throw IllegalArgumentException when id is invalid`() {
        val id = "invalid"

        val result = assertFailure {
            AccountIdFactory.of(id)
        }

        result.hasMessage(
            "Expected either a 36-char string in the standard hex-and-dash UUID format or a 32-char " +
                "hexadecimal string, but was \"invalid\" of length 7",
        )
        result.isInstanceOf<IllegalArgumentException>()
    }

    @Test
    fun `new should return AccountId with a uuid`() {
        val result = AccountIdFactory.create()

        assertThat(result.asRaw()).isUuid()
    }

    @Test
    fun `create should return AccountId with unique ids`() {
        val ids = List(10) { AccountIdFactory.create().asRaw() }

        ids.forEachIndexed { index, id ->
            ids.drop(index + 1).forEach { otherId ->
                assertThat(id).isNotEqualTo(otherId)
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun Assert<String>.isUuid() = given { actual ->
        Uuid.parse(actual)
    }
}
