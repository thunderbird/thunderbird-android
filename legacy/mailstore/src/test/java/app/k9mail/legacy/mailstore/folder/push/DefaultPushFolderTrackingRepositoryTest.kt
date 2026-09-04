package app.k9mail.legacy.mailstore.folder.push

import app.cash.turbine.test
import app.k9mail.legacy.mailstore.FolderSettingsChangedListener
import app.k9mail.legacy.mailstore.ListenableMessageStore
import app.k9mail.legacy.mailstore.MessageStoreFactory
import app.k9mail.legacy.mailstore.MessageStoreManager
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import net.thunderbird.account.fake.FakeAccountData.ACCOUNT_ID_OTHER_RAW
import net.thunderbird.account.fake.FakeAccountData.ACCOUNT_ID_RAW
import net.thunderbird.core.android.account.AccountRemovedListener
import net.thunderbird.core.android.account.AccountsChangeListener
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.android.account.LegacyAccountDtoManager
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.mail.folder.api.data.FolderError
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class DefaultPushFolderTrackingRepositoryTest {
    private val account = LegacyAccountDto(ACCOUNT_ID_RAW)
    private val accountId = account.id
    private val messageStore = mock<ListenableMessageStore>()
    private val accountManager = FakeLegacyAccountDtoManager(accounts = listOf(account))
    private val messageStoreFactory = FakeMessageStoreFactory(
        messageStoresByUuid = mapOf(account.uuid to messageStore),
    )
    private val messageStoreManager = MessageStoreManager(accountManager, messageStoreFactory)
    private val testSubject = DefaultPushFolderTrackingRepository(
        logger = TestLogger(),
        messageStoreManager = messageStoreManager,
        ioDispatcher = Dispatchers.Unconfined,
    )

    @Test
    fun `isEnabled should return Success with true when account has a push enabled folder`() = runTest {
        // Arrange
        whenever(messageStore.hasPushEnabledFolder()).thenReturn(true)

        // Act
        val result = testSubject.isEnabled(accountId)

        // Assert
        assertThat(result).isEqualTo(Outcome.success(true))
    }

    @Test
    fun `isEnabled should return Success with false when account has no push enabled folder`() = runTest {
        // Arrange
        whenever(messageStore.hasPushEnabledFolder()).thenReturn(false)

        // Act
        val result = testSubject.isEnabled(accountId)

        // Assert
        assertThat(result).isEqualTo(Outcome.success(false))
    }

    @Test
    fun `isEnabled should return Failure with AccountNotFound when account does not exist`() = runTest {
        // Arrange
        val unknownAccountId = AccountIdFactory.of(ACCOUNT_ID_OTHER_RAW)

        // Act
        val result = testSubject.isEnabled(unknownAccountId)

        // Assert
        assertThat(result).isEqualTo(Outcome.failure(FolderError.AccountNotFound))
    }

    @Test
    fun `disable should disable push on the message store and return Success`() = runTest {
        // Act
        val result = testSubject.disable(accountId)

        // Assert
        verify(messageStore).setPushDisabled()
        assertThat(result).isEqualTo(Outcome.success(Unit))
    }

    @Test
    fun `disable should return Failure with AccountNotFound when account does not exist`() = runTest {
        // Arrange
        val unknownAccountId = AccountIdFactory.of(ACCOUNT_ID_OTHER_RAW)

        // Act
        val result = testSubject.disable(unknownAccountId)

        // Assert
        assertThat(result).isEqualTo(Outcome.failure(FolderError.AccountNotFound))
    }

    @Test
    fun `observeEnabled should emit the current push enabled state`() = runTest {
        // Arrange
        whenever(messageStore.hasPushEnabledFolder()).thenReturn(true)

        // Act & Assert
        testSubject.observeEnabled(accountId).test {
            assertThat(awaitItem()).isEqualTo(Outcome.success(true))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeEnabled should emit an updated state when folder settings change`() = runTest {
        // Arrange
        whenever(messageStore.hasPushEnabledFolder()).thenReturn(false)
        val listenerCaptor = argumentCaptor<FolderSettingsChangedListener>()

        // Act & Assert
        testSubject.observeEnabled(accountId).test {
            assertThat(awaitItem()).isEqualTo(Outcome.success(false))

            verify(messageStore).addFolderSettingsChangedListener(listenerCaptor.capture())
            whenever(messageStore.hasPushEnabledFolder()).thenReturn(true)
            listenerCaptor.firstValue.onFolderSettingsChanged()

            assertThat(awaitItem()).isEqualTo(Outcome.success(true))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeEnabled should emit Failure with AccountNotFound when account does not exist`() = runTest {
        // Arrange
        val unknownAccountId = AccountIdFactory.of(ACCOUNT_ID_OTHER_RAW)

        // Act & Assert
        testSubject.observeEnabled(unknownAccountId).test {
            assertThat(awaitItem()).isInstanceOf<Outcome.Failure<FolderError>>()
            cancelAndIgnoreRemainingEvents()
        }
    }
}

private class FakeLegacyAccountDtoManager(
    accounts: List<LegacyAccountDto> = emptyList(),
) : LegacyAccountDtoManager {
    private val accountsByUuid = accounts.associateBy { it.uuid }

    override fun getAccounts(): List<LegacyAccountDto> = accountsByUuid.values.toList()
    override fun getAccountsFlow(): Flow<List<LegacyAccountDto>> = flowOf(getAccounts())
    override fun getAccount(accountUuid: String): LegacyAccountDto? = accountsByUuid[accountUuid]
    override fun getAccountFlow(accountUuid: String): Flow<LegacyAccountDto?> = flowOf(getAccount(accountUuid))
    override fun addAccountRemovedListener(listener: AccountRemovedListener) = Unit
    override fun moveAccount(account: LegacyAccountDto, newPosition: Int) = Unit
    override fun addOnAccountsChangeListener(accountsChangeListener: AccountsChangeListener) = Unit
    override fun removeOnAccountsChangeListener(accountsChangeListener: AccountsChangeListener) = Unit
    override fun saveAccount(account: LegacyAccountDto) = Unit
}

private class FakeMessageStoreFactory(
    private val messageStoresByUuid: Map<String, ListenableMessageStore>,
) : MessageStoreFactory {
    override fun create(account: LegacyAccountDto): ListenableMessageStore = messageStoresByUuid.getValue(account.uuid)
}
