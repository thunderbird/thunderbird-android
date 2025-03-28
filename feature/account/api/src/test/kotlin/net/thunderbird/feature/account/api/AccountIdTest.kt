package net.thunderbird.feature.account.api

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

class AccountIdTest {

    @Test
    fun `from should return AccountId with the same id`() {
        val id = "123e4567-e89b-12d3-a456-426614174000"

        val result = AccountId.from(id)

        assertThat(result.value).isEqualTo(id)
    }

    @Test
    fun `from should throw IllegalArgumentException when id is invalid`() {
        val id = "invalid"

        val result = assertFailure {
            AccountId.from(id)
        }

        result.hasMessage("Invalid AccountId: $id")
        result.isInstanceOf<IllegalArgumentException>()
    }

    @Test
    fun `create should return AccountId with a uuid`() {
        val result = AccountId.create()

        assertThat(result.value).isUuid()
    }

    @Test
    fun `create should return AccountId with unique ids`() {
        val ids = List(10) { AccountId.create().value }

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
