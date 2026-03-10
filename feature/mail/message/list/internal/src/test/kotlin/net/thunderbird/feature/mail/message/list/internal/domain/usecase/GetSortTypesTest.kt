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
import net.thunderbird.feature.mail.message.list.internal.fakes.FakeLegacyAccount
import net.thunderbird.feature.mail.message.list.internal.fakes.FakeLegacyAccountManager
import net.thunderbird.feature.mail.message.list.ui.state.SortType
import net.thunderbird.core.android.account.SortType as DomainSortType

@OptIn(ExperimentalCoroutinesApi::class)
class GetSortTypesTest {

    @Test
    fun `invoke should return default sort type when accounts are empty`() = runTest {
        // Arrange
        val defaultSortType = SortType.DateDesc
        val getDefaultSortType = spy(
            obj = DomainContract.UseCase.GetDefaultSortType { defaultSortType },
        )
        val useCase = createTestSubject(
            accounts = emptyList(),
            getDefaultSortType = getDefaultSortType,
        )

        // Act
        val result = useCase(accountIds = emptySet())

        // Assert
        val expected = mapOf(null to defaultSortType)
        assertThat(result).isEqualTo(expected)
        verifySuspend { getDefaultSortType() }
    }

    @Test
    fun `invoke should return map with default and account specific sort types`() = runTest {
        // Arrange
        val defaultSortType = SortType.DateDesc
        val accountId1 = AccountIdFactory.create()
        val accountId2 = AccountIdFactory.create()

        val account1 = FakeLegacyAccount(id = accountId1).copy(
            sortType = DomainSortType.SORT_SUBJECT,
            sortAscending = mapOf(DomainSortType.SORT_SUBJECT to true),
        )
        val account2 = FakeLegacyAccount(id = accountId2).copy(
            sortType = DomainSortType.SORT_DATE,
            sortAscending = mapOf(DomainSortType.SORT_DATE to false),
        )

        val getDefaultSortType = spy(
            obj = DomainContract.UseCase.GetDefaultSortType { defaultSortType },
        )
        val useCase = createTestSubject(
            accounts = listOf(account1, account2),
            getDefaultSortType = getDefaultSortType,
        )

        // Act
        val result = useCase(accountIds = emptySet())

        // Assert
        val expected = mapOf(
            null to defaultSortType,
            accountId1 to SortType.SubjectAsc,
            accountId2 to SortType.DateDesc,
        )
        assertThat(result).isEqualTo(expected)
        verifySuspend { getDefaultSortType() }
    }

    @Test
    fun `invoke should use false for ascending if not found in sortAscending map`() = runTest {
        // Arrange
        val defaultSortType = SortType.DateDesc
        val accountId = AccountIdFactory.create()

        val account = FakeLegacyAccount(id = accountId).copy(
            sortType = DomainSortType.SORT_ARRIVAL,
            sortAscending = emptyMap(),
        )

        val getDefaultSortType = spy(
            obj = DomainContract.UseCase.GetDefaultSortType { defaultSortType },
        )
        val useCase = createTestSubject(
            accounts = listOf(account),
            getDefaultSortType = getDefaultSortType,
        )

        // Act
        val result = useCase(accountIds = emptySet())

        // Assert
        val expected = mapOf(
            null to defaultSortType,
            accountId to SortType.ArrivalDesc,
        )
        assertThat(result).isEqualTo(expected)
        verifySuspend { getDefaultSortType() }
    }

    private fun createTestSubject(
        accounts: List<LegacyAccount>,
        getDefaultSortType: DomainContract.UseCase.GetDefaultSortType,
    ): GetSortTypes = GetSortTypes(
        accountManager = FakeLegacyAccountManager(accounts = accounts),
        getDefaultSortType = getDefaultSortType,
        ioDispatcher = UnconfinedTestDispatcher(),
    )
}
