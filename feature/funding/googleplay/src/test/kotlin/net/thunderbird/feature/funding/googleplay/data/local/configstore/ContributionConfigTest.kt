package net.thunderbird.feature.funding.googleplay.data.local.configstore

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import assertk.assertions.isNull
import org.junit.Test

class ContributionConfigTest {

    @Test
    fun `DEFAULT should have null lastPurchasedContribution`() {
        assertThat(ContributionConfig.DEFAULT.lastPurchasedContribution).isNull()
    }

    @Test
    fun `constructor should set properties correctly`() {
        val purchase = ContributionPurchase(
            id = "id",
            title = "title",
            description = "description",
            price = 1000L,
            priceFormatted = "$1.00",
            productId = "product_id",
            orderId = "order_id",
            purchaseTimeMillis = 123456789L,
        )

        val config = ContributionConfig(lastPurchasedContribution = purchase)

        assertThat(config.lastPurchasedContribution).isEqualTo(purchase)
    }

    @Test
    fun `equals and hashCode should work correctly`() {
        val purchase1 = ContributionPurchase(
            id = "id",
            title = "title",
            description = "description",
            price = 1000L,
            priceFormatted = "$1.00",
            productId = "product_id",
            orderId = "order_id",
            purchaseTimeMillis = 123456789L,
        )
        val purchase2 = purchase1.copy(id = "other")

        val config1 = ContributionConfig(lastPurchasedContribution = purchase1)
        val config2 = ContributionConfig(lastPurchasedContribution = purchase1)
        val config3 = ContributionConfig(lastPurchasedContribution = purchase2)
        val config4 = ContributionConfig(lastPurchasedContribution = null)

        assertThat(config1).isEqualTo(config2)
        assertThat(config1.hashCode()).isEqualTo(config2.hashCode())
        assertThat(config1).isNotEqualTo(config3)
        assertThat(config1).isNotEqualTo(config4)
    }

    @Test
    fun `copy should allow modifying properties`() {
        val purchase = ContributionPurchase(
            id = "id",
            title = "title",
            description = "description",
            price = 1000L,
            priceFormatted = "$1.00",
            productId = "product_id",
            orderId = "order_id",
            purchaseTimeMillis = 123456789L,
        )
        val config = ContributionConfig(lastPurchasedContribution = null)

        val updatedConfig = config.copy(lastPurchasedContribution = purchase)

        assertThat(updatedConfig.lastPurchasedContribution).isEqualTo(purchase)
    }
}
