package net.thunderbird.feature.funding.googleplay.data.local.configstore

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.configstore.ConfigId
import net.thunderbird.core.configstore.backend.ConfigBackend
import net.thunderbird.core.configstore.backend.ConfigBackendProvider
import net.thunderbird.core.configstore.testing.TestConfigBackend
import net.thunderbird.core.logging.testing.TestLogger
import org.junit.Test

class ContributionConfigStoreTest {

    private val mapper = ContributionConfigMapper(
        logger = TestLogger(),
    )
    private val migration = ContributionConfigMigration()
    private val definition = ContributionConfigDefinition(
        mapper = mapper,
        migration = migration,
    )

    @Test
    fun `should return default config when store is empty`() = runTest {
        val backend = TestConfigBackend()
        val provider = FakeConfigBackendProvider(backend)
        val testSubject = ContributionConfigStore(provider, definition)

        val result = testSubject.config.first()

        assertThat(result).isEqualTo(ContributionConfig.DEFAULT)
        assertThat(result.lastPurchasedContribution).isNull()
    }

    @Test
    fun `should return stored config when store populated`() = runTest {
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
        val backend = TestConfigBackend(
            mapper.toConfig(
                ContributionConfig(lastPurchasedContribution = purchase),
            ),
        )
        val provider = FakeConfigBackendProvider(backend)
        val testSubject = ContributionConfigStore(provider, definition)

        val result = testSubject.config.first()

        assertThat(result.lastPurchasedContribution).isEqualTo(purchase)
    }

    @Test
    fun `update should store new config in backend`() = runTest {
        val backend = TestConfigBackend()
        val provider = FakeConfigBackendProvider(backend)
        val testSubject = ContributionConfigStore(provider, definition)
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

        testSubject.update { current ->
            (current ?: ContributionConfig.DEFAULT).copy(lastPurchasedContribution = purchase)
        }

        val storedConfig = backend.read(definition.keys).first()
        val result = mapper.fromConfig(storedConfig)
        assertThat(result?.lastPurchasedContribution).isEqualTo(purchase)
    }

    private class FakeConfigBackendProvider(private val backend: ConfigBackend) : ConfigBackendProvider {
        override fun provide(id: ConfigId): ConfigBackend = backend
    }
}
