package net.thunderbird.feature.funding.googleplay.domain.policy

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test
import net.thunderbird.feature.funding.googleplay.domain.entity.ContributionId
import net.thunderbird.feature.funding.googleplay.domain.entity.OneTimeContribution
import net.thunderbird.feature.funding.googleplay.domain.entity.RecurringContribution

class ContributionPreselectorTest {

    private val preselector = ContributionPreselector()

    @Test
    fun `preselect should pick second lowest price for both types when multiple items exist`() {
        // Arrange
        val oneTimeContributions = listOf(
            createOneTimeContribution("1", 1000L),
            createOneTimeContribution("2", 2000L),
            createOneTimeContribution("3", 3000L),
        )
        val recurringContributions = listOf(
            createRecurringContribution("4", 1000L),
            createRecurringContribution("5", 2000L),
            createRecurringContribution("6", 3000L),
        )

        // Act
        val result = preselector.preselect(oneTimeContributions, recurringContributions)

        // Assert
        assertThat(result.oneTimeId).isEqualTo(ContributionId("2"))
        assertThat(result.recurringId).isEqualTo(ContributionId("5"))
    }

    @Test
    fun `preselect should pick item only when only one item exists`() {
        // Arrange
        val oneTimeContributions = listOf(createOneTimeContribution("1", 1000L))
        val recurringContributions = listOf(createRecurringContribution("2", 1000L))

        // Act
        val result = preselector.preselect(oneTimeContributions, recurringContributions)

        // Assert
        assertThat(result.oneTimeId).isEqualTo(ContributionId("1"))
        assertThat(result.recurringId).isEqualTo(ContributionId("2"))
    }

    @Test
    fun `preselect should return null when list is empty`() {
        // Arrange
        val oneTimeContributions = emptyList<OneTimeContribution>()
        val recurringContributions = emptyList<RecurringContribution>()

        // Act
        val result = preselector.preselect(oneTimeContributions, recurringContributions)

        // Assert
        assertThat(result.oneTimeId).isNull()
        assertThat(result.recurringId).isNull()
    }

    @Test
    fun `preselect should sort contributions by price before picking second lowest`() {
        // Arrange
        val oneTimeContributions = listOf(
            createOneTimeContribution("3", 3000L),
            createOneTimeContribution("1", 1000L),
            createOneTimeContribution("2", 2000L),
        )
        val recurringContributions = listOf(
            createRecurringContribution("6", 3000L),
            createRecurringContribution("4", 1000L),
            createRecurringContribution("5", 2000L),
        )

        // Act
        val result = preselector.preselect(oneTimeContributions, recurringContributions)

        // Assert
        assertThat(result.oneTimeId).isEqualTo(ContributionId("2"))
        assertThat(result.recurringId).isEqualTo(ContributionId("5"))
    }

    private fun createOneTimeContribution(id: String, price: Long) = OneTimeContribution(
        id = ContributionId(id),
        title = "Title $id",
        description = "Description $id",
        price = price,
        priceFormatted = "$price",
    )

    private fun createRecurringContribution(id: String, price: Long) = RecurringContribution(
        id = ContributionId(id),
        title = "Title $id",
        description = "Description $id",
        price = price,
        priceFormatted = "$price",
    )
}
