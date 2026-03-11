package net.thunderbird.feature.funding.googleplay.data.local

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.funding.googleplay.data.FundingDataContract.Local
import net.thunderbird.feature.funding.googleplay.data.local.configstore.ContributionConfig
import net.thunderbird.feature.funding.googleplay.data.local.configstore.ContributionPurchase
import net.thunderbird.feature.funding.googleplay.data.mapper.ContributionPurchaseMapper
import org.junit.Test
import net.thunderbird.feature.funding.googleplay.data.FundingDataContract.Mapper.ContributionPurchase as ContributionPurchaseMapperContract

class LocalContributionPurchaseDataSourceTest {

    private val fakeConfigStore = FakeContributionConfigStore()
    private val contributionPurchaseMapper: ContributionPurchaseMapperContract = ContributionPurchaseMapper()
    private val testSubject = LocalContributionPurchaseDataSource(
        configStore = fakeConfigStore,
        contributionPurchaseMapper = contributionPurchaseMapper,
    )

    @Test
    fun `get should return last purchased contribution from config store`() = runTest {
        // Arrange
        val purchase = createContributionPurchase()
        fakeConfigStore.setConfig(ContributionConfig(lastPurchasedContribution = purchase))

        // Act
        val result = testSubject.get().first()

        // Assert
        when (result) {
            is Outcome.Success -> assertThat(result.data).isEqualTo(
                contributionPurchaseMapper.mapToPurchasedContribution(purchase),
            )

            else -> throw AssertionError("Expected Success, got $result")
        }
    }

    @Test
    fun `get should return null when no contribution is purchased`() = runTest {
        // Arrange
        fakeConfigStore.setConfig(ContributionConfig(lastPurchasedContribution = null))

        // Act
        val result = testSubject.get().first()

        // Assert
        when (result) {
            is Outcome.Success -> assertThat(result.data).isNull()
            else -> throw AssertionError("Expected Success, got $result")
        }
    }

    @Test
    fun `update should update config store with new purchase`() = runTest {
        // Arrange
        val purchase = contributionPurchaseMapper.mapToPurchasedContribution(createContributionPurchase())

        // Act
        val result = testSubject.update(purchase)

        // Assert
        when (result) {
            is Outcome.Success -> assertThat(
                fakeConfigStore.getCurrentConfig().lastPurchasedContribution,
            ).isEqualTo(contributionPurchaseMapper.mapToContributionPurchase(purchase))

            else -> throw AssertionError("Expected Success, got $result")
        }
    }

    private fun createContributionPurchase() = ContributionPurchase(
        id = "id",
        title = "title",
        description = "description",
        price = 1000L,
        priceFormatted = "$1.00",
        productId = "product_id",
        orderId = "order_id",
        purchaseTimeMillis = 123456789L,
    )

    private class FakeContributionConfigStore : Local.ContributionConfigStore {
        private val _config = MutableStateFlow(ContributionConfig.DEFAULT)
        override val config = _config

        fun setConfig(config: ContributionConfig) {
            _config.value = config
        }

        fun getCurrentConfig() = _config.value

        override suspend fun update(transform: (ContributionConfig?) -> ContributionConfig) {
            _config.value = transform(_config.value)
        }

        override suspend fun clear() {
            _config.value = ContributionConfig.DEFAULT
        }
    }
}
