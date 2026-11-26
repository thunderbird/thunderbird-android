package net.thunderbird.feature.mail.storage.globaldb.migration

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import kotlin.test.Test
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
        assertFailure {
            AppMigrationState.InProgress(-0.1f)
        }.isInstanceOf<IllegalArgumentException>()
    }

    @Test
    fun `InProgress rejects progress above range`() {
        assertFailure {
            AppMigrationState.InProgress(1.1f)
        }.isInstanceOf<IllegalArgumentException>()
    }

    @Test
    fun `InProgress rejects NaN progress`() {
        assertFailure {
            AppMigrationState.InProgress(Float.NaN)
        }.isInstanceOf<IllegalArgumentException>()
    }

    @Test
    fun `InProgress rejects infinite progress`() {
        assertFailure {
            AppMigrationState.InProgress(Float.POSITIVE_INFINITY)
        }.isInstanceOf<IllegalArgumentException>()
    }
}
