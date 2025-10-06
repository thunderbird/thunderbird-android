package com.fsck.k9

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.preferences.UnifiedInboxConfigurator
import net.thunderbird.core.android.account.AccountManager
import net.thunderbird.core.preference.GeneralSettings
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.core.preference.display.DisplaySettings
import net.thunderbird.core.preference.display.inboxSettings.DisplayInboxSettings
import org.junit.After
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
    private lateinit var generalSettingsManager: GeneralSettingsManager
    private lateinit var configurator: UnifiedInboxConfigurator

    @Before
    fun setUp() {
        accountManager = mock(AccountManager::class.java)
        generalSettingsManager =
            FakeGeneralSettingsManager(
                GeneralSettings(
                    display = DisplaySettings(
                        inboxSettings = DisplayInboxSettings(
                            isShowUnifiedInbox = false,
                        ),
                    ),
                ),
            )
        configurator = UnifiedInboxConfigurator(accountManager, generalSettingsManager)

        startKoin {
            modules(
                module {
                    single { generalSettingsManager } // No need for DefaultGeneralSettingsManager here
                },
            )
        }
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
        assertThat(generalSettingsManager.getConfig().display.inboxSettings.isShowUnifiedInbox).isEqualTo(true)
    }

    @Test
    fun `configureUnifiedInbox should not enable unified inbox when there are less than two accounts`() {
        // Given
        `when`(accountManager.getAccounts()).thenReturn(listOf(mock()))

        // When
        configurator.configureUnifiedInbox()

        // Then
        assertThat(generalSettingsManager.getConfig().display.inboxSettings.isShowUnifiedInbox).isEqualTo(false)
    }

    @Test
    fun `configureUnifiedInbox should not enable unified inbox when there are more than two accounts`() {
        // Given
        `when`(accountManager.getAccounts()).thenReturn(listOf(mock(), mock(), mock()))

        // When
        configurator.configureUnifiedInbox()

        // Then
        assertThat(generalSettingsManager.getConfig().display.inboxSettings.isShowUnifiedInbox).isEqualTo(false)
    }
}

private class FakeGeneralSettingsManager(private var generalSettings: GeneralSettings) : GeneralSettingsManager {
    override fun getSettings() = error("Not implemented")

    override fun getSettingsFlow() = error("Not implemented")

    override fun save(config: GeneralSettings) {
        generalSettings = config
    }

    override fun getConfig() = generalSettings

    override fun getConfigFlow() = error("Not implemented")
}
