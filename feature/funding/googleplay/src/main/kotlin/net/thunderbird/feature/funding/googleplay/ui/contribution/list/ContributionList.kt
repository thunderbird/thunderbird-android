package net.thunderbird.feature.funding.googleplay.ui.contribution.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import app.k9mail.core.ui.compose.common.resources.annotatedStringResource
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import net.thunderbird.components.ui.bolt.atom.Surface
import net.thunderbird.components.ui.bolt.atom.button.ButtonSegmentedSingleChoice
import net.thunderbird.components.ui.bolt.atom.button.ButtonText
import net.thunderbird.components.ui.bolt.atom.icon.Icon
import net.thunderbird.components.ui.bolt.atom.icon.Icons
import net.thunderbird.components.ui.bolt.atom.text.TextBodyLarge
import net.thunderbird.components.ui.bolt.atom.text.TextBodyMedium
import net.thunderbird.components.ui.bolt.atom.text.TextBodySmall
import net.thunderbird.components.ui.bolt.atom.text.TextLabelLarge
import net.thunderbird.components.ui.bolt.molecule.ContentLoadingErrorView
import net.thunderbird.components.ui.bolt.molecule.LoadingView
import net.thunderbird.components.ui.bolt.theme.BoltTheme
import net.thunderbird.feature.funding.googleplay.R
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.ContributionError
import net.thunderbird.feature.funding.googleplay.domain.entity.Contribution
import net.thunderbird.feature.funding.googleplay.domain.entity.ContributionId
import net.thunderbird.feature.funding.googleplay.ui.contribution.ContributionListItem
import net.thunderbird.feature.funding.googleplay.ui.contribution.list.ContributionListSliceContract.ContributionType
import net.thunderbird.feature.funding.googleplay.ui.contribution.list.ContributionListSliceContract.Event
import net.thunderbird.feature.funding.googleplay.ui.contribution.list.ContributionListSliceContract.State
import net.thunderbird.feature.funding.googleplay.ui.contribution.mapErrorToTitle

@Composable
internal fun ContributionList(
    state: State,
    onEvent: (Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = BoltTheme.colors.surfaceContainerLowest,
        shape = BoltTheme.shapes.small,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .padding(BoltTheme.spacings.double),
            verticalArrangement = Arrangement.spacedBy(BoltTheme.spacings.default),
        ) {
            TextLabelLarge(
                text = stringResource(R.string.funding_googleplay_contribution_list_title),
            )

            ContentLoadingErrorView(
                state = state,
                loading = {
                    LoadingView()
                },
                error = { error ->
                    ListErrorView(
                        error = error,
                        onRetryClick = {
                            onEvent(Event.RetryClicked)
                        },
                    )
                },
                content = { state ->
                    if (state.contributions.oneTimeContributions.isEmpty() &&
                        state.contributions.recurringContributions.isEmpty()
                    ) {
                        ListEmptyView()
                    } else {
                        ListContentView(
                            state = state,
                            onContributionTypeClick = {
                                onEvent(Event.TypeClicked(it))
                            },
                            onItemClick = {
                                onEvent(Event.ItemClicked(it.id))
                            },
                        )
                    }
                },
            )

            TextBodyMedium(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(stringResource(R.string.funding_googleplay_contribution_list_disclaimer))
                    }
                },
                modifier = Modifier.padding(top = BoltTheme.spacings.default),
            )
        }
    }
}

/**
 * Lays the contribution items out on a grid of equally sized cells that fills the available width.
 *
 * A row that is not filled completely keeps the cell width of the rows above and is centered below them, instead of
 * having its items stretched over the full width.
 */
@Composable
private fun ChoicesGrid(
    contributions: ImmutableList<Contribution>,
    onItemClick: (Contribution) -> Unit,
    selectedItemId: ContributionId?,
    modifier: Modifier = Modifier,
) {
    val spacing = BoltTheme.spacings.default

    Layout(
        content = {
            contributions.forEach {
                ContributionListItem(
                    text = it.priceFormatted,
                    onClick = { onItemClick(it) },
                    isSelected = it.id == selectedItemId,
                )
            }
        },
        modifier = modifier,
    ) { measurables, constraints ->
        if (measurables.isEmpty()) {
            return@Layout layout(width = 0, height = 0) {}
        }

        val spacingPx = spacing.roundToPx()
        val widestItem = measurables.maxOf { it.maxIntrinsicWidth(Constraints.Infinity) }
        val itemsPerRow = itemsPerRow(constraints, widestItem, spacingPx, measurables.size)
        val width = if (constraints.hasBoundedWidth) {
            constraints.maxWidth
        } else {
            itemsPerRow * widestItem + (itemsPerRow - 1) * spacingPx
        }

        // The division remainder goes to the leftmost columns, so a filled row uses the width exactly.
        val widthForItems = width - spacingPx * (itemsPerRow - 1)
        val itemWidth = widthForItems / itemsPerRow
        val remainder = widthForItems % itemsPerRow

        val rows = measurables
            .mapIndexed { index, measurable ->
                val columnWidth = if (index % itemsPerRow < remainder) itemWidth + 1 else itemWidth
                measurable.measure(constraints.copy(minWidth = columnWidth, maxWidth = columnWidth))
            }
            .chunked(itemsPerRow)
        val rowHeight = rows.maxOf { row -> row.maxOf { it.height } }

        layout(width = width, height = rows.size * rowHeight + (rows.size - 1) * spacingPx) {
            rows.forEachIndexed { rowIndex, row ->
                val rowWidth = row.sumOf { it.width } + (row.size - 1) * spacingPx
                var x = (width - rowWidth) / 2

                row.forEach { placeable ->
                    placeable.placeRelative(x = x, y = rowIndex * (rowHeight + spacingPx))
                    x += placeable.width + spacingPx
                }
            }
        }
    }
}

/**
 * The number of items that fit into a row when every item is as wide as the widest one.
 */
private fun itemsPerRow(constraints: Constraints, widestItem: Int, spacing: Int, itemCount: Int): Int {
    if (!constraints.hasBoundedWidth) {
        return itemCount
    }

    return ((constraints.maxWidth + spacing) / (widestItem + spacing)).coerceIn(1, itemCount)
}

@Composable
private fun ListContentView(
    state: State,
    onContributionTypeClick: (ContributionType) -> Unit,
    onItemClick: (Contribution) -> Unit,
    modifier: Modifier = Modifier,
) {
    val oneTimeLabel = stringResource(R.string.funding_googleplay_contribution_list_type_one_time)
    val recurringLabel = stringResource(R.string.funding_googleplay_contribution_list_type_recurring)

    Column(
        verticalArrangement = Arrangement.spacedBy(BoltTheme.spacings.default),
        modifier = modifier,
    ) {
        ButtonSegmentedSingleChoice(
            options = ContributionType.entries.toImmutableList(),
            selectedOption = state.selectedType,
            onClick = onContributionTypeClick,
            optionTitle = { type ->
                when (type) {
                    ContributionType.OneTime -> oneTimeLabel
                    ContributionType.Recurring -> recurringLabel
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        ChoicesGrid(
            contributions = if (state.selectedType == ContributionType.Recurring) {
                state.contributions.recurringContributions
            } else {
                state.contributions.oneTimeContributions
            },
            selectedItemId = state.selectedContribution?.id,
            onItemClick = onItemClick,
        )
    }
}

@Composable
private fun ListEmptyView(
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(BoltTheme.spacings.double),
        modifier = modifier.padding(vertical = BoltTheme.spacings.double),
    ) {
        val annotatedString = annotatedStringResource(
            id = R.string.funding_googleplay_contribution_list_empty_message,
            argument = buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        color = BoltTheme.colors.primary,
                        textDecoration = TextDecoration.Underline,
                    ),
                ) {
                    withLink(
                        LinkAnnotation.Url(
                            url = stringResource(R.string.funding_googleplay_thunderbird_website_url),
                        ),
                    ) {
                        append(stringResource(R.string.funding_googleplay_thunderbird_website_domain))
                    }
                }
            },
        )

        TextBodyMedium(
            text = stringResource(R.string.funding_googleplay_contribution_list_empty_title),
        )

        TextBodyMedium(
            text = annotatedString,
        )
    }
}

@Composable
private fun ListErrorView(
    error: ContributionError,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val showDetails = remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(BoltTheme.spacings.default),
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BoltTheme.spacings.half),
        ) {
            TextBodyLarge(
                text = mapErrorToTitle(error),
            )
            if (error.message.isNotEmpty()) {
                Icon(
                    imageVector = if (showDetails.value) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = "Show more details",
                    modifier = Modifier
                        .clickable { showDetails.value = !showDetails.value }
                        .padding(BoltTheme.spacings.quarter),
                )
            }

            AnimatedVisibility(visible = showDetails.value) {
                TextBodySmall(
                    text = error.message,
                    color = BoltTheme.colors.onErrorContainer,
                )
            }
        }

        ButtonText(
            text = stringResource(R.string.funding_googleplay_contribution_list_error_retry_button),
            onClick = onRetryClick,
            modifier = Modifier.padding(top = BoltTheme.spacings.default),
        )
    }
}
