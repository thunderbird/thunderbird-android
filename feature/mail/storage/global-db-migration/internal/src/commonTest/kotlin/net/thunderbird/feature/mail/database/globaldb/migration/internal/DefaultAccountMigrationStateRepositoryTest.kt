package net.thunderbird.feature.mail.database.globaldb.migration.internal

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.mail.storage.globaldb.migration.AccountMigrationState
import net.thunderbird.feature.mail.storage.globaldb.migration.AppMigrationState
import net.thunderbird.feature.mail.storage.globaldb.migration.internal.DefaultAccountMigrationStateRepository

internal class DefaultAccountMigrationStateRepositoryTest {

    private val fakeRepository = FakeAppMigrationStateRepository()

    private val testSubject = DefaultAccountMigrationStateRepository(
        appMigrationStateRepository = fakeRepository,
    )

    @BeforeTest
    fun setup() = runTest {
        fakeRepository.update(AppMigrationState.NotStarted)
    }

    @Test
    fun `should return NotStarted when app migration is not started`() = runTest {
        val accountId = AccountIdFactory.create()

        val result = testSubject.getByAccountId(accountId)

        assertThat(result).isEqualTo(AccountMigrationState.NotStarted)
    }

    @Test
    fun `should return Completed when app migration is completed`() = runTest {
        val accountId = AccountIdFactory.create()
        fakeRepository.update(AppMigrationState.Completed)

        val result = testSubject.getByAccountId(accountId)

        assertThat(result).isEqualTo(AccountMigrationState.Completed)
    }
}
