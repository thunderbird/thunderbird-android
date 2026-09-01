package net.thunderbird.feature.funding.googleplay.data.mapper

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import net.thunderbird.feature.funding.googleplay.data.local.configstore.ContributionPurchase
import net.thunderbird.feature.funding.googleplay.domain.entity.ContributionId
import net.thunderbird.feature.funding.googleplay.domain.entity.OneTimeContribution
import net.thunderbird.feature.funding.googleplay.domain.entity.PurchasedContribution
import org.junit.Test

internal class ContributionPurchaseMapperTest {

    private val testSubject = ContributionPurchaseMapper()

    @Test
    fun `mapToContributionPurchase should map purchased one-time contribution`() {
        // Arrange
        val purchasedContribution = createPurchasedContribution()

        // Act
        val result = testSubject.mapToContributionPurchase(purchasedContribution)

        // Assert
        assertThat(result).isEqualTo(
            ContributionPurchase(
                id = "one_time_1",
                title = "Title 1",
                description = "Description 1",
                price = 100L,
                priceFormatted = "$1.00",
                productId = "one_time_1",
                orderId = null,
                purchaseTimeMillis = purchasedContribution.purchaseDate
                    .toInstant(TimeZone.currentSystemDefault())
                    .toEpochMilliseconds(),
            ),
        )
    }

    @Test
    fun `mapToPurchasedContribution should map contribution purchase`() {
        // Arrange
        val contributionPurchase = ContributionPurchase(
            id = "one_time_1",
            title = "Title 1",
            description = "Description 1",
            price = 100L,
            priceFormatted = "$1.00",
            productId = "one_time_1",
            orderId = "order_1",
            purchaseTimeMillis = 1_717_236_800_000,
        )

        // Act
        val result = testSubject.mapToPurchasedContribution(contributionPurchase)

        // Assert
        assertThat(result).isEqualTo(
            PurchasedContribution(
                id = ContributionId("one_time_1"),
                contribution = OneTimeContribution(
                    id = ContributionId("one_time_1"),
                    title = "Title 1",
                    description = "Description 1",
                    price = 100L,
                    priceFormatted = "$1.00",
                ),
                purchaseDate = Instant.fromEpochMilliseconds(contributionPurchase.purchaseTimeMillis)
                    .toLocalDateTime(TimeZone.currentSystemDefault()),
            ),
        )
    }

    private fun createPurchasedContribution() = PurchasedContribution(
        id = ContributionId("one_time_1"),
        contribution = OneTimeContribution(
            id = ContributionId("one_time_1"),
            title = "Title 1",
            description = "Description 1",
            price = 100L,
            priceFormatted = "$1.00",
        ),
        purchaseDate = LocalDateTime(2024, 6, 1, 12, 0),
    )
}
