package net.thunderbird.feature.mail.storage.globaldb.migration

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlin.test.assertFailsWith

class AccountMigrationStateTest {

    @Test
    fun `InProgress accepts progress at range boundaries`() {
        assertThat(AccountMigrationState.InProgress(0.0f).progress).isEqualTo(0.0f)
        assertThat(AccountMigrationState.InProgress(1.0f).progress).isEqualTo(1.0f)
    }

    @Test
    fun `InProgress accepts progress within range`() {
        assertThat(AccountMigrationState.InProgress(0.5f).progress).isEqualTo(0.5f)
    }

    @Test
    fun `InProgress rejects progress below range`() {
        assertFailsWith<IllegalArgumentException> {
            AccountMigrationState.InProgress(-0.1f)
        }
    }

    @Test
    fun `InProgress rejects progress above range`() {
        assertFailsWith<IllegalArgumentException> {
            AccountMigrationState.InProgress(1.1f)
        }
    }

    @Test
    fun `InProgress rejects NaN progress`() {
        assertFailsWith<IllegalArgumentException> {
            AccountMigrationState.InProgress(Float.NaN)
        }
    }

    @Test
    fun `InProgress rejects infinite progress`() {
        assertFailsWith<IllegalArgumentException> {
            AccountMigrationState.InProgress(Float.POSITIVE_INFINITY)
        }
    }
}
