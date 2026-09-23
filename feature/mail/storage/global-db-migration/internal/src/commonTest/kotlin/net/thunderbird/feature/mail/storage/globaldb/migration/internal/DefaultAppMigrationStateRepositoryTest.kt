package net.thunderbird.feature.mail.storage.globaldb.migration.internal

import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import net.thunderbird.feature.mail.storage.globaldb.migration.AppMigrationState

internal class DefaultAppMigrationStateRepositoryTest {

    private val testSubject = DefaultAppMigrationStateRepository()

    @Test
    fun `should return NotStarted when app migration is not started`() = runTest {
        val result = testSubject.get()
        assert(result == AppMigrationState.NotStarted)
    }
}
