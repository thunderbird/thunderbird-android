package net.thunderbird.feature.account

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.serialization.json.Json

class AccountIdTest {

    private val rawUuid = "123e4567-e89b-12d3-a456-426614174000"
    private val json = Json

    @Test
    fun `should serialize AccountId to JSON string`() {
        val accountId = AccountIdFactory.of(rawUuid)

        val serialized = json.encodeToString(AccountId.serializer(), accountId)

        assertThat(serialized).isEqualTo("\"$rawUuid\"")
    }

    @Test
    fun `should deserialize AccountId from JSON string`() {
        val expectedAccountId = AccountIdFactory.of(rawUuid)

        val deserialized = json.decodeFromString(AccountId.serializer(), "\"$rawUuid\"")

        assertThat(deserialized).isEqualTo(expectedAccountId)
    }
}
