package net.thunderbird.feature.funding.googleplay.ui.contribution.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.testing.ComposeTest
import app.k9mail.core.ui.compose.testing.setContentWithTheme
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isCloseTo
import assertk.assertions.isGreaterThanOrEqualTo
import kotlin.test.Test
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import net.thunderbird.feature.funding.googleplay.domain.entity.AvailableContributions
import net.thunderbird.feature.funding.googleplay.domain.entity.Contribution
import net.thunderbird.feature.funding.googleplay.domain.entity.ContributionId
import net.thunderbird.feature.funding.googleplay.domain.entity.OneTimeContribution
import net.thunderbird.feature.funding.googleplay.domain.entity.RecurringContribution
import net.thunderbird.feature.funding.googleplay.ui.contribution.FakeData
import net.thunderbird.feature.funding.googleplay.ui.contribution.list.ContributionListSliceContract.ContributionType
import net.thunderbird.feature.funding.googleplay.ui.contribution.list.ContributionListSliceContract.State
import org.robolectric.annotation.Config

private const val TOLERANCE = 1f

/**
 * The minimum touch target size recommended by the Material accessibility guidelines.
 */
private const val MIN_TOUCH_TARGET_DP = 48f

/**
 * How many contribution items share a row depends on the available width and on how wide the longest price label is.
 * The width is therefore set explicitly per test instead of relying on the simulated screen, which is kept large
 * enough to never constrain the content.
 */
@Config(qualifiers = "w1920dp-h1600dp-mdpi")
internal class ContributionListKtTest : ComposeTest() {

    @Test
    fun `should keep all items in a single row while they fit`() = runComposeTest {
        // Arrange
        val contributions = FakeData.oneTimeContributions

        // Act
        setContentWithTheme {
            ContributionListOfWidth(width = 420.dp, oneTimeContributions = contributions)
        }

        // Assert
        val rows = itemRows(contributions)
        assertThat(rows).hasSize(1)
        assertThat(rows[0]).hasSize(contributions.size)
        assertAllItemsShareTheSameWidth(rows)
    }

    @Test
    fun `should not stretch the items of a partially filled last row`() = runComposeTest {
        // Arrange
        val contributions = FakeData.oneTimeContributions

        // Act
        setContentWithTheme {
            ContributionListOfWidth(width = 320.dp, oneTimeContributions = contributions)
        }

        // Assert
        val rows = itemRows(contributions)
        assertThat(rows).hasSize(2)
        assertThat(rows[1]).hasSize(2)
        assertAllItemsShareTheSameWidth(rows)
    }

    @Test
    fun `should center a partially filled last row below the row above`() = runComposeTest {
        // Arrange
        val contributions = FakeData.oneTimeContributions

        // Act
        setContentWithTheme {
            ContributionListOfWidth(width = 320.dp, oneTimeContributions = contributions)
        }

        // Assert
        val rows = itemRows(contributions)
        assertThat(centerOf(rows[1])).isCloseTo(centerOf(rows[0]), TOLERANCE)
    }

    @Test
    fun `should not stretch a single item in the last row`() = runComposeTest {
        // Arrange
        val contributions = FakeData.oneTimeContributions

        // Act
        setContentWithTheme {
            ContributionListOfWidth(width = 360.dp, oneTimeContributions = contributions)
        }

        // Assert
        val rows = itemRows(contributions)
        assertThat(rows).hasSize(2)
        assertThat(rows[1]).hasSize(1)
        assertAllItemsShareTheSameWidth(rows)
    }

    @Test
    fun `should center a single item in the last row below the row above`() = runComposeTest {
        // Arrange
        val contributions = FakeData.oneTimeContributions

        // Act
        setContentWithTheme {
            ContributionListOfWidth(width = 360.dp, oneTimeContributions = contributions)
        }

        // Assert
        val rows = itemRows(contributions)
        assertThat(centerOf(rows[1])).isCloseTo(centerOf(rows[0]), TOLERANCE)
    }

    @Test
    fun `should center the last row when longer price labels reduce the items per row`() = runComposeTest {
        // Arrange
        val contributions = longLabelContributions()

        // Act
        setContentWithTheme {
            ContributionListOfWidth(width = 420.dp, oneTimeContributions = contributions)
        }

        // Assert
        val rows = itemRows(contributions)
        assertThat(rows).hasSize(2)
        assertThat(rows[1]).hasSize(1)
        assertAllItemsShareTheSameWidth(rows)
        assertThat(centerOf(rows[1])).isCloseTo(centerOf(rows[0]), TOLERANCE)
    }

    @Test
    fun `should center the last row of the recurring contributions as well`() = runComposeTest {
        // Arrange
        val contributions = FakeData.recurringContributions

        // Act
        setContentWithTheme {
            ContributionListOfWidth(
                width = 320.dp,
                oneTimeContributions = FakeData.oneTimeContributions,
                recurringContributions = contributions,
                selectedType = ContributionType.Recurring,
            )
        }

        // Assert
        val rows = itemRows(contributions)
        assertThat(rows).hasSize(2)
        assertAllItemsShareTheSameWidth(rows)
        assertThat(centerOf(rows[1])).isCloseTo(centerOf(rows[0]), TOLERANCE)
    }

    @Test
    fun `should lay out a single contribution centered in one row`() = runComposeTest {
        // Arrange
        val contributions = FakeData.oneTimeContributions.take(1).toImmutableList()
        val width = 360.dp

        // Act
        setContentWithTheme {
            ContributionListOfWidth(width = width, oneTimeContributions = contributions)
        }

        // Assert
        val rows = itemRows(contributions)
        assertThat(rows).hasSize(1)
        assertThat(rows[0]).hasSize(1)
        assertThat(centerOf(rows[0])).isCloseTo(width.value / 2, TOLERANCE)
    }

    @Test
    fun `should give two contributions the same width in one row`() = runComposeTest {
        // Arrange
        val contributions = FakeData.oneTimeContributions.take(2).toImmutableList()

        // Act
        setContentWithTheme {
            ContributionListOfWidth(width = 360.dp, oneTimeContributions = contributions)
        }

        // Assert
        val rows = itemRows(contributions)
        assertThat(rows).hasSize(1)
        assertAllItemsShareTheSameWidth(rows)
    }

    @Test
    fun `should keep every item wide enough to touch on a narrow screen`() = runComposeTest {
        // Arrange
        val contributions = FakeData.oneTimeContributions

        // Act
        setContentWithTheme {
            ContributionListOfWidth(width = 200.dp, oneTimeContributions = contributions)
        }

        // Assert
        itemRows(contributions).flatten().forEach { bounds ->
            assertThat(bounds.width).isGreaterThanOrEqualTo(MIN_TOUCH_TARGET_DP)
        }
    }
}

@Composable
private fun ContributionListOfWidth(
    width: Dp,
    oneTimeContributions: ImmutableList<OneTimeContribution>,
    recurringContributions: ImmutableList<RecurringContribution> = FakeData.recurringContributions,
    selectedType: ContributionType = ContributionType.OneTime,
) {
    Box(modifier = Modifier.width(width)) {
        ContributionList(
            state = State(
                contributions = AvailableContributions(
                    oneTimeContributions = oneTimeContributions,
                    recurringContributions = recurringContributions,
                    preselection = FakeData.preselection,
                ),
                selectedType = selectedType,
                isLoading = false,
            ),
            onEvent = {},
        )
    }
}

private fun longLabelContributions(): ImmutableList<OneTimeContribution> {
    val prices = listOf("1.099,00 €", "549,00 €", "329,00 €", "219,00 €", "109,00 €", "65,00 €")

    return prices
        .mapIndexed { index, price ->
            OneTimeContribution(
                id = ContributionId("contribution_long_label_$index"),
                title = "One time payment: $price",
                description = "One time payment of $price",
                price = 1000L * (index + 1),
                priceFormatted = price,
            )
        }
        .toImmutableList()
}

private data class ItemBounds(val left: Float, val top: Float, val right: Float) {
    val width: Float = right - left
}

/**
 * The bounds of every contribution item, grouped into the rows they were laid out in.
 */
private fun ComposeContentTestRule.itemRows(contributions: List<Contribution>): List<List<ItemBounds>> {
    return contributions
        .map { contribution ->
            onNodeWithText(contribution.priceFormatted).getUnclippedBoundsInRoot().let {
                ItemBounds(left = it.left.value, top = it.top.value, right = it.right.value)
            }
        }
        .groupBy { it.top }
        .values
        .toList()
}

private fun centerOf(row: List<ItemBounds>): Float = (row.first().left + row.last().right) / 2

private fun assertAllItemsShareTheSameWidth(rows: List<List<ItemBounds>>) {
    val expectedWidth = rows.first().first().width

    rows.flatten().forEach { bounds ->
        assertThat(bounds.width).isCloseTo(expectedWidth, TOLERANCE)
    }
}
