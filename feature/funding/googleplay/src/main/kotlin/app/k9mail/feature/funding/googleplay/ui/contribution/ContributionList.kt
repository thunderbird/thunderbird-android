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
import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ContributionList(
    contributions: ImmutableList<Contribution>,
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = MainTheme.spacings.default),
                horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
            ) {
                ContributionListItem(
                    text = stringResource(R.string.funding_googleplay_contribution_list_type_one_time),
                    onClick = onOneTimeContributionTypeClick,
                    isSelected = !isRecurringContributionSelected,
                    modifier = Modifier.weight(1f),
                )
                ContributionListItem(
                    text = stringResource(R.string.funding_googleplay_contribution_list_type_recurring),
                    onClick = onRecurringContributionTypeClick,
                    isSelected = isRecurringContributionSelected,
                    modifier = Modifier.weight(1f),
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
                verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
            ) {
                contributions.forEach {
                    ContributionListItem(
                        text = it.price,
                        onClick = { onItemClick(it) },
                        isSelected = it == selectedItem,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            TextBodyMedium(
                text = stringResource(R.string.funding_googleplay_contribution_list_disclaimer),
                modifier = Modifier.padding(top = MainTheme.spacings.default),
            )
        }
    }
}
