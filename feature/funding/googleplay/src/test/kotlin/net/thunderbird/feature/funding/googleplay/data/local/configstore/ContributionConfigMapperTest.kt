package net.thunderbird.feature.funding.googleplay.data.local.configstore

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import net.thunderbird.core.configstore.Config
import net.thunderbird.core.logging.LogLevel
import net.thunderbird.core.logging.testing.TestLogger
import org.junit.Test

class ContributionConfigMapperTest {

    private val logger = TestLogger()
    private val mapper = ContributionConfigMapper(logger)

    @Test
    fun `toConfig should serialize lastPurchasedContribution and be reversible`() {
        val purchase = ContributionPurchase(
            id = "id123",
            title = "Awesome Contribution",
            description = "Thank you!",
            price = 500L,
            priceFormatted = "\$5.00",
            productId = "prod_1",
            orderId = "order_1",
            purchaseTimeMillis = 1000L,
        )
        val config = ContributionConfig(lastPurchasedContribution = purchase)

        val result = mapper.toConfig(config)

        val storedJson = result[ContributionConfigKeys.LAST_PURCHASED_CONTRIBUTION]
        assertThat(storedJson).isNotNull()

        val roundTripped = mapper.fromConfig(
            Config().apply {
                set(ContributionConfigKeys.LAST_PURCHASED_CONTRIBUTION, storedJson!!)
            },
        )
        assertThat(roundTripped?.lastPurchasedContribution).isEqualTo(purchase)
    }

    @Test
    fun `toConfig should handle null lastPurchasedContribution`() {
        val config = ContributionConfig(lastPurchasedContribution = null)

        val result = mapper.toConfig(config)

        val storedJson = result[ContributionConfigKeys.LAST_PURCHASED_CONTRIBUTION]
        assertThat(storedJson).isEqualTo("null")
    }

    @Test
    fun `fromConfig should deserialize a config produced by toConfig`() {
        val purchase = ContributionPurchase(
            id = "id123",
            title = "Awesome Contribution",
            description = "Thank you!",
            price = 500L,
            priceFormatted = "\$5.00",
            productId = "prod_1",
            orderId = "order_1",
            purchaseTimeMillis = 1000L,
        )
        val cfg = mapper.toConfig(ContributionConfig(lastPurchasedContribution = purchase))

        val result = mapper.fromConfig(cfg)

        assertThat(result?.lastPurchasedContribution).isEqualTo(purchase)
    }

    @Test
    fun `fromConfig should return default when key is missing`() {
        val config = Config()

        val result = mapper.fromConfig(config)

        assertThat(result).isEqualTo(ContributionConfig.DEFAULT)
        assertThat(result?.lastPurchasedContribution).isNull()
    }

    @Test
    fun `fromConfig should return default and log error when JSON is malformed`() {
        val config = Config().apply {
            set(ContributionConfigKeys.LAST_PURCHASED_CONTRIBUTION, "{ invalid json }")
        }

        val result = mapper.fromConfig(config)

        assertThat(result).isEqualTo(ContributionConfig.DEFAULT)
        val errorEvents = logger.events.filter { it.level == LogLevel.ERROR }
        assertThat(errorEvents.size).isGreaterThan(0)
    }
}
