package net.thunderbird.feature.account

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test

class UnifiedAccountIdTest {

    @Test
    fun `unified account id is nil uuid`() {
        assertThat(UnifiedAccountId.toString()).isEqualTo("00000000-0000-0000-0000-000000000000")
    }

    @Test
    fun `isUnified returns true for unified account id`() {
        assertThat(UnifiedAccountId.isUnified).isTrue()
    }

    @Test
    fun `isUnified returns false for non-unified account id`() {
        val nonUnifiedAccountId = AccountIdFactory.of("123e4567-e89b-12d3-a456-426614174000")
        assertThat(nonUnifiedAccountId.isUnified).isFalse()
    }

    @Test
    fun `requireReal returns the same id if not unified`() {
        val nonUnifiedAccountId = AccountIdFactory.of("123e4567-e89b-12d3-a456-426614174000")
        assertThat(nonUnifiedAccountId.requireReal()).isEqualTo(nonUnifiedAccountId)
    }

    @Test
    fun `requireReal throws exception if unified`() {
        assertFailure {
            UnifiedAccountId.requireReal()
        }.hasMessage("Operation not allowed on unified account")
    }
}
