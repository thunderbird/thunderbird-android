package com.fsck.k9

import com.fsck.k9.preferences.RealGeneralSettingsManager
import com.fsck.k9.preferences.UnifiedInboxConfigurator
import net.thunderbird.core.android.account.AccountManager
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.context.GlobalContext.stopKoin
import org.koin.dsl.module
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class UnifiedInboxConfiguratorTest {

    private lateinit var accountManager: AccountManager
    private lateinit var configurator: UnifiedInboxConfigurator

    @Before
    fun setUp() {
        accountManager = mock(AccountManager::class.java)
        configurator = UnifiedInboxConfigurator(accountManager)

        // Start Koin with a minimal module
        startKoin {
            modules(
                module {
                    single { mock(RealGeneralSettingsManager::class.java) }
                },
            )
        }

        // Reset K9 settings to avoid state leakage across tests
        K9.isShowUnifiedInbox = false
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `configureUnifiedInbox should enable unified inbox when there are exactly two accounts`() {
        // Given
        `when`(accountManager.getAccounts()).thenReturn(listOf(mock(), mock()))

        // When
        configurator.configureUnifiedInbox()

        // Then
        assertTrue(K9.isShowUnifiedInbox)
    }

    @Test
    fun `configureUnifiedInbox should not enable unified inbox when there are less than two accounts`() {
        // Given
        `when`(accountManager.getAccounts()).thenReturn(listOf(mock()))

        // When
        configurator.configureUnifiedInbox()

        // Then
        assertTrue(!K9.isShowUnifiedInbox)
    }

    @Test
    fun `configureUnifiedInbox should not enable unified inbox when there are more than two accounts`() {
        // Given
        `when`(accountManager.getAccounts()).thenReturn(listOf(mock(), mock(), mock()))

        // When
        configurator.configureUnifiedInbox()

        // Then
        assertTrue(!K9.isShowUnifiedInbox)
    }
}
