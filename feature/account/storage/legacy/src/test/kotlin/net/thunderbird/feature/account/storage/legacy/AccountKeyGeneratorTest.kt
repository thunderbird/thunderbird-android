package net.thunderbird.feature.account.storage.legacy

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEqualTo
import kotlin.test.Test
import net.thunderbird.account.fake.FakeAccountData

class AccountKeyGeneratorTest {

    @Test
    fun `create should combine account ID with key`() {
        // Arrange
        val accountId = FakeAccountData.ACCOUNT_ID
        val testSubject = AccountKeyGenerator(accountId)
        val key = "testKey"

        // Act
        val result = testSubject.create(key)

        // Assert
        assertThat(result).isEqualTo("$accountId.$key")
    }

    @Test
    fun `create should fail with empty key`() {
        // Arrange
        val accountId = FakeAccountData.ACCOUNT_ID
        val testSubject = AccountKeyGenerator(accountId)
        val key = ""

        // Act & Assert
        assertFailure {
            testSubject.create(key)
        }.isInstanceOf<IllegalArgumentException>()
            .hasMessage("Key must not be empty")
    }

    @Test
    fun `create should work with different account IDs`() {
        // Arrange
        val accountId1 = FakeAccountData.ACCOUNT_ID
        val accountId2 = FakeAccountData.ACCOUNT_ID_OTHER
        val testSubject1 = AccountKeyGenerator(accountId1)
        val testSubject2 = AccountKeyGenerator(accountId2)
        val key = "testKey"

        // Act
        val result1 = testSubject1.create(key)
        val result2 = testSubject2.create(key)

        // Assert
        assertThat(result1).isEqualTo("$accountId1.$key")
        assertThat(result2).isEqualTo("$accountId2.$key")
        assertThat(result1).isNotEqualTo(result2)
    }
}
