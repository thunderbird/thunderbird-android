package app.k9mail.feature.funding.googleplay.ui.contribution

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextLabelLarge
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.feature.funding.googleplay.R
import app.k9mail.feature.funding.googleplay.domain.entity.Contribution
import app.k9mail.feature.funding.googleplay.domain.entity.OneTimeContribution
import app.k9mail.feature.funding.googleplay.domain.entity.RecurringContribution
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun ContributionList(
    oneTimeContributions: ImmutableList<OneTimeContribution>,
    recurringContributions: ImmutableList<RecurringContribution>,
    selectedItem: Contribution?,
    isRecurringContributionSelected: Boolean,
    onOneTimeContributionTypeClick: () -> Unit,
    onRecurringContributionTypeClick: () -> Unit,
    onItemClick: (Contribution) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MainTheme.colors.surfaceContainerLowest,
        shape = MainTheme.shapes.small,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .padding(MainTheme.spacings.double),
            verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
        ) {
            TextLabelLarge(
                text = stringResource(R.string.funding_googleplay_contribution_list_title),
            )

            TypeSelectionRow(
                oneTimeContributions = oneTimeContributions,
                recurringContributions = recurringContributions,
                isRecurringContributionSelected = isRecurringContributionSelected,
                onOneTimeContributionTypeClick = onOneTimeContributionTypeClick,
                onRecurringContributionTypeClick = onRecurringContributionTypeClick,
            )

            ChoicesRow(
                contributions = if (isRecurringContributionSelected) recurringContributions else oneTimeContributions,
                selectedItem = selectedItem,
                onItemClick = onItemClick,
            )

            TextBodyMedium(
                text = stringResource(R.string.funding_googleplay_contribution_list_disclaimer),
                modifier = Modifier.padding(top = MainTheme.spacings.default),
            )
        }
    }
}

@Composable
private fun TypeSelectionRow(
    oneTimeContributions: ImmutableList<OneTimeContribution>,
    recurringContributions: ImmutableList<RecurringContribution>,
    isRecurringContributionSelected: Boolean,
    onOneTimeContributionTypeClick: () -> Unit,
    onRecurringContributionTypeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = MainTheme.spacings.default),
        horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
    ) {
        if (oneTimeContributions.isEmpty() && recurringContributions.isEmpty()) {
            ContributionListItem(
                text = stringResource(R.string.funding_googleplay_contribution_list_type_none_available),
                onClick = {},
                isSelected = true,
                modifier = Modifier.weight(1f),
            )
        } else {
            if (oneTimeContributions.isNotEmpty()) {
                ContributionListItem(
                    text = stringResource(R.string.funding_googleplay_contribution_list_type_one_time),
                    onClick = onOneTimeContributionTypeClick,
                    isSelected = !isRecurringContributionSelected,
                    modifier = Modifier.weight(1f),
                )
            }
            if (recurringContributions.isNotEmpty()) {
                ContributionListItem(
                    text = stringResource(R.string.funding_googleplay_contribution_list_type_recurring),
                    onClick = onRecurringContributionTypeClick,
                    isSelected = isRecurringContributionSelected,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChoicesRow(
    contributions: ImmutableList<Contribution>,
    onItemClick: (Contribution) -> Unit,
    selectedItem: Contribution?,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
        verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
        modifier = modifier,
    ) {
        contributions.forEach {
            ContributionListItem(
                text = it.priceFormatted,
                onClick = { onItemClick(it) },
                isSelected = it == selectedItem,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
