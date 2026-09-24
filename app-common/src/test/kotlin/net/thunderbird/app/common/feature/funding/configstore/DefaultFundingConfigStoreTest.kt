package net.thunderbird.app.common.feature.funding.configstore

import kotlin.apply
import kotlin.test.DefaultAsserter.assertEquals
import kotlin.test.Test
import kotlin.test.assertEquals
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

    val defaultConfig = FundingConfig(
        lastFundingReminderShownActivityAmount = 0L,
        fundingReminderCount = 0,
    )

    @Test
    fun `config should provide the expected default`() = runTest {
        assertEquals(defaultConfig, FundingConfig(0L, 0))
    }

    @Test
    fun `config should start with default values`() = runTest {
        // Setup test subject and expected values
        val backend = getDefaultTestConfigBackend()
        val configTestSubject = createTestSubject(backend)

        // Get and test initial value
        val initialTestValue = configTestSubject.config.first()
        assertEquals("Backend should set initial values.", defaultConfig, initialTestValue)
    }

    @Test
    fun `config should update values`() = runTest {
        // Setup test subject and expected values
        val backend = getDefaultTestConfigBackend()
        val configTestSubject = createTestSubject(backend)
        val newConfig = FundingConfig(
            lastFundingReminderShownActivityAmount = 10000L,
            fundingReminderCount = 1,
        )

        this.backgroundScope.launch {
            // Update test subject
            configTestSubject.update {
                val oldConfig = it ?: FundingConfig.DEFAULT
                oldConfig.copy(
                    lastFundingReminderShownActivityAmount = newConfig.lastFundingReminderShownActivityAmount,
                    fundingReminderCount = newConfig.fundingReminderCount,
                )
            }

            // Ensure test subject has new values
            val testConfig = configTestSubject.config.first()
            assertEquals(
                newConfig.lastFundingReminderShownActivityAmount,
                testConfig.lastFundingReminderShownActivityAmount,
            )
            assertEquals(
                newConfig.fundingReminderCount,
                testConfig.fundingReminderCount,
            )
        }
    }

    private fun createTestSubject(backend: ConfigBackend): FundingConfigStore = DefaultFundingConfigStore(
        id = CONFIG_ID,
        provider = ConfigBackendProvider(backend),
        scope = TestScope(),
    )

    private fun getDefaultTestConfigBackend() = TestConfigBackend(
        initialConfig = Config().apply {
            this[FundingConfigKeys.LAST_FUNDING_REMINDER_SHOWN_ACTIVITY_AMOUNT] = 0L
            this[FundingConfigKeys.FUNDING_REMINDER_COUNT] = 0
        },
    )

    private companion object {
        val CONFIG_ID = ConfigId(backend = "test", feature = "fundingconfig")
    }
}

private class ConfigBackendProvider(
    private val backend: ConfigBackend,
) : ConfigBackendProvider {
    override fun provide(id: ConfigId): ConfigBackend = backend
}
