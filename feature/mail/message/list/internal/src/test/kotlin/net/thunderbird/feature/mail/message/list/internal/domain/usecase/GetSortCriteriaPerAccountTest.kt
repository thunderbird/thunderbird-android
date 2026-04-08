package net.thunderbird.feature.mail.message.list.internal.domain.usecase

import assertk.assertThat
import assertk.assertions.isEqualTo
import dev.mokkery.spy
import dev.mokkery.verifySuspend
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.mail.message.list.domain.DomainContract
import net.thunderbird.feature.mail.message.list.domain.model.SortCriteria
import net.thunderbird.feature.mail.message.list.domain.model.SortType
import net.thunderbird.feature.mail.message.list.internal.fakes.FakeLegacyAccount
import net.thunderbird.feature.mail.message.list.internal.fakes.FakeLegacyAccountManager
import net.thunderbird.core.android.account.SortType as DomainSortType

@OptIn(ExperimentalCoroutinesApi::class)
class GetSortCriteriaPerAccountTest {

    @Test
    fun `invoke should return default sort type when accounts are empty`() = runTest {
        // Arrange
        val defaultSortCriteria = SortCriteria(primary = SortType.DateDesc)
        val getDefaultSortCriteria = spy(
            obj = DomainContract.UseCase.GetDefaultSortCriteria { defaultSortCriteria },
        )
        val useCase = createTestSubject(
            accounts = emptyList(),
            getDefaultSortCriteria = getDefaultSortCriteria,
        )

        // Act
        val result = useCase(accountIds = emptySet())

        // Assert
        val expected = mapOf(null to defaultSortCriteria)
        assertThat(result).isEqualTo(expected)
        verifySuspend { getDefaultSortCriteria() }
    }

    @Test
    fun `invoke should return map with default and account specific sort types`() = runTest {
        // Arrange
        val defaultSortCriteria = SortCriteria(primary = SortType.DateDesc)
        val accountId1 = AccountIdFactory.create()
        val accountId2 = AccountIdFactory.create()

        val account1 = FakeLegacyAccount(id = accountId1).copy(
            sortType = DomainSortType.SORT_SUBJECT,
            sortAscending = mapOf(
                DomainSortType.SORT_SUBJECT to true,
                DomainSortType.SORT_DATE to true,
            ),
        )
        val account2 = FakeLegacyAccount(id = accountId2).copy(
            sortType = DomainSortType.SORT_DATE,
            sortAscending = mapOf(DomainSortType.SORT_DATE to false),
        )

        val getDefaultSortCriteria = spy(
            obj = DomainContract.UseCase.GetDefaultSortCriteria { defaultSortCriteria },
        )
        val useCase = createTestSubject(
            accounts = listOf(account1, account2),
            getDefaultSortCriteria = getDefaultSortCriteria,
        )

        // Act
        val result = useCase(accountIds = emptySet())

        // Assert
        val expected = mapOf(
            null to defaultSortCriteria,
            accountId1 to SortCriteria(primary = SortType.SubjectAsc, secondary = SortType.DateAsc),
            accountId2 to SortCriteria(primary = SortType.DateDesc),
        )
        assertThat(result).isEqualTo(expected)
        verifySuspend { getDefaultSortCriteria() }
    }

    @Test
    fun `invoke should use false for ascending if not found in sortAscending map`() = runTest {
        // Arrange
        val defaultSortCriteria = SortCriteria(primary = SortType.DateDesc)
        val accountId = AccountIdFactory.create()

        val account = FakeLegacyAccount(id = accountId).copy(
            sortType = DomainSortType.SORT_ARRIVAL,
            sortAscending = emptyMap(),
        )

        val getDefaultSortCriteria = spy(
            obj = DomainContract.UseCase.GetDefaultSortCriteria { defaultSortCriteria },
        )
        val useCase = createTestSubject(
            accounts = listOf(account),
            getDefaultSortCriteria = getDefaultSortCriteria,
        )

        // Act
        val result = useCase(accountIds = emptySet())

        // Assert
        val expected = mapOf(
            null to defaultSortCriteria,
            accountId to SortCriteria(primary = SortType.ArrivalDesc),
        )
        assertThat(result).isEqualTo(expected)
        verifySuspend { getDefaultSortCriteria() }
    }

    private fun createTestSubject(
        accounts: List<LegacyAccount>,
        getDefaultSortCriteria: DomainContract.UseCase.GetDefaultSortCriteria,
    ): GetSortCriteriaPerAccount = GetSortCriteriaPerAccount(
        accountManager = FakeLegacyAccountManager(accounts = accounts),
        getDefaultSortCriteria = getDefaultSortCriteria,
        ioDispatcher = UnconfinedTestDispatcher(),
    )
}
