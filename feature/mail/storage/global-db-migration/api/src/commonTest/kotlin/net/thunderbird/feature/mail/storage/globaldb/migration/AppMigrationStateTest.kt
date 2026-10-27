package net.thunderbird.feature.mail.storage.globaldb.migration

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlin.test.assertFailsWith
import net.thunderbird.feature.mail.storage.globaldb.migration.AppMigrationState

class AppMigrationStateTest {

    @Test
    fun `InProgress accepts progress at range boundaries`() {
        assertThat(AppMigrationState.InProgress(0.0f).progress).isEqualTo(0.0f)
        assertThat(AppMigrationState.InProgress(1.0f).progress).isEqualTo(1.0f)
    }

    @Test
    fun `InProgress accepts progress within range`() {
        assertThat(AppMigrationState.InProgress(0.5f).progress).isEqualTo(0.5f)
    }

    @Test
    fun `InProgress rejects progress below range`() {
        assertFailsWith<IllegalArgumentException> {
            AppMigrationState.InProgress(-0.1f)
        }
    }

    @Test
    fun `InProgress rejects progress above range`() {
        assertFailsWith<IllegalArgumentException> {
            AppMigrationState.InProgress(1.1f)
        }
    }

    @Test
    fun `InProgress rejects NaN progress`() {
        assertFailsWith<IllegalArgumentException> {
            AppMigrationState.InProgress(Float.NaN)
        }
    }

    @Test
    fun `InProgress rejects infinite progress`() {
        assertFailsWith<IllegalArgumentException> {
            AppMigrationState.InProgress(Float.POSITIVE_INFINITY)
        }
    }
}
