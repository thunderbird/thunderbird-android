package net.thunderbird.app.common.feature.funding.configstore

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.apply
import kotlin.test.Test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.configstore.Config
import net.thunderbird.core.configstore.ConfigId
import net.thunderbird.core.configstore.backend.ConfigBackend
import net.thunderbird.core.configstore.backend.ConfigBackendProvider
import net.thunderbird.core.configstore.testing.TestConfigBackend
import net.thunderbird.feature.funding.api.FundingConfig
import net.thunderbird.feature.funding.api.FundingConfigKeys
import net.thunderbird.feature.funding.api.FundingConfigStore

class DefaultFundingConfigStoreTest {

    @Test
    fun `config should start with default values provided to the backend`() = runTest {
        // Setup test subject and expected values
        val backend = getDefaultTestConfigBackend()
        val configTestSubject = createTestSubject(backend)

        // Get and test initial value
        val initialTestValue = configTestSubject.config.first()
        assertThat(initialTestValue).isEqualTo(DEFAULT_CONFIG)
    }

    @Test
    fun `config should update values`() = runTest {
        // Setup test subject and expected values
        val backend = getDefaultTestConfigBackend()
        val configTestSubject = createTestSubject(backend)
        val newConfig = FundingConfig(
            reminderReferenceTimestamp = 0L,
            lastFundingReminderShownTimestamp = 10000L,
            fundingReminderCount = 1,
        )

        this.backgroundScope.launch {
            // Update test subject
            configTestSubject.update {
                val oldConfig = it ?: FundingConfig.DEFAULT
                oldConfig.copy(
                    lastFundingReminderShownTimestamp = newConfig.lastFundingReminderShownTimestamp,
                    fundingReminderCount = newConfig.fundingReminderCount,
                )
            }

            // Ensure test subject has new values
            val testConfig = configTestSubject.config.first()
            assertThat(testConfig.lastFundingReminderShownTimestamp)
                .isEqualTo(newConfig.lastFundingReminderShownTimestamp)
            assertThat(testConfig.fundingReminderCount).isEqualTo(newConfig.fundingReminderCount)
        }
    }

    private fun createTestSubject(backend: ConfigBackend): FundingConfigStore = DefaultFundingConfigStore(
        id = CONFIG_ID,
        provider = ConfigBackendProvider(backend),
        scope = TestScope(),
    )

    private fun getDefaultTestConfigBackend() = TestConfigBackend(
        initialConfig = Config().apply {
            this[FundingConfigKeys.REMINDER_REFERENCE_TIMESTAMP] = DEFAULT_CONFIG.reminderReferenceTimestamp ?: 0L
            this[FundingConfigKeys.LAST_FUNDING_REMINDER_SHOWN] = DEFAULT_CONFIG.lastFundingReminderShownTimestamp ?: 0L
            this[FundingConfigKeys.FUNDING_REMINDER_COUNT] = DEFAULT_CONFIG.fundingReminderCount ?: 0
        },
    )

    private companion object {
        val CONFIG_ID = ConfigId(backend = "test", feature = "fundingconfig")

        // The default config uses null values as a placeholder to load data. This is the loaded data
        val DEFAULT_CONFIG = FundingConfig(
            reminderReferenceTimestamp = 0L,
            lastFundingReminderShownTimestamp = 0L,
            fundingReminderCount = 0,
        )
    }
}

private class ConfigBackendProvider(
    private val backend: ConfigBackend,
) : ConfigBackendProvider {
    override fun provide(id: ConfigId): ConfigBackend = backend
}
