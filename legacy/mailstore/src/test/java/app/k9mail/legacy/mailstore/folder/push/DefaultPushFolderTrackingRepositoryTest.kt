package app.k9mail.legacy.mailstore.folder.push

import app.cash.turbine.test
import app.k9mail.legacy.mailstore.FakeMessageStoreFactory
import app.k9mail.legacy.mailstore.FolderSettingsChangedListener
import app.k9mail.legacy.mailstore.ListenableMessageStore
import app.k9mail.legacy.mailstore.MessageStoreManager
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import net.thunderbird.account.fake.FakeAccountData.ACCOUNT_ID
import net.thunderbird.account.fake.FakeAccountData.ACCOUNT_ID_OTHER_RAW
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.components.core.outcome.Outcome
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.mail.folder.api.data.FolderError
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class DefaultPushFolderTrackingRepositoryTest {
    private val accountId = ACCOUNT_ID
    private val messageStore = mock<ListenableMessageStore>()
    private val messageStoreFactory = FakeMessageStoreFactory(
        messageStoresById = mapOf(accountId to messageStore),
    )
    private val messageStoreManager = MessageStoreManager(messageStoreFactory)
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
        assertThat(result).isInstanceOf(Outcome.Failure::class)
        assertThat((result as Outcome.Failure).error).isInstanceOf(FolderError.AccountNotFound::class)
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
        assertThat(result).isInstanceOf(Outcome.Failure::class)
        assertThat((result as Outcome.Failure).error).isInstanceOf(FolderError.AccountNotFound::class)
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
