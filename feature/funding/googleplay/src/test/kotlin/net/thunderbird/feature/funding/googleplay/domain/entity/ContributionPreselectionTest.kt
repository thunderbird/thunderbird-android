package net.thunderbird.feature.funding.googleplay.domain.entity

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test

class ContributionPreselectionTest {

    @Test
    fun `select should return recurringId when isRecurring is true`() {
        // Arrange
        val oneTimeId = ContributionId("one_time_id")
        val recurringId = ContributionId("recurring_id")
        val preselection = ContributionPreselection(
            oneTimeId = oneTimeId,
            recurringId = recurringId,
        )

        // Act
        val result = preselection.select(isRecurring = true)

        // Assert
        assertThat(result).isEqualTo(recurringId)
    }

    @Test
    fun `select should return oneTimeId when isRecurring is false`() {
        // Arrange
        val oneTimeId = ContributionId("one_time_id")
        val recurringId = ContributionId("recurring_id")
        val preselection = ContributionPreselection(
            oneTimeId = oneTimeId,
            recurringId = recurringId,
        )

        // Act
        val result = preselection.select(isRecurring = false)

        // Assert
        assertThat(result).isEqualTo(oneTimeId)
    }

    @Test
    fun `select should return null when isRecurring is true and recurringId is null`() {
        // Arrange
        val preselection = ContributionPreselection(
            oneTimeId = ContributionId("one_time_id"),
            recurringId = null,
        )

        // Act
        val result = preselection.select(isRecurring = true)

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `select should return null when isRecurring is false and oneTimeId is null`() {
        // Arrange
        val preselection = ContributionPreselection(
            oneTimeId = null,
            recurringId = ContributionId("recurring_id"),
        )

        // Act
        val result = preselection.select(isRecurring = false)

        // Assert
        assertThat(result).isNull()
    }
}
