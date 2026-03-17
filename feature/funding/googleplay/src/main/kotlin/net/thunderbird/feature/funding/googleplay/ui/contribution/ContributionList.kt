package net.thunderbird.feature.funding.googleplay.ui.contribution

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import app.k9mail.core.ui.compose.common.resources.annotatedStringResource
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonSegmentedSingleChoice
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonText
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyLarge
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodySmall
import app.k9mail.core.ui.compose.designsystem.atom.text.TextLabelLarge
import app.k9mail.core.ui.compose.designsystem.molecule.ContentLoadingErrorView
import app.k9mail.core.ui.compose.designsystem.molecule.LoadingView
import kotlinx.collections.immutable.ImmutableList
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons
import net.thunderbird.core.ui.compose.theme2.MainTheme
import net.thunderbird.feature.funding.googleplay.R
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.ContributionError
import net.thunderbird.feature.funding.googleplay.domain.entity.Contribution
import net.thunderbird.feature.funding.googleplay.domain.entity.ContributionId
import net.thunderbird.feature.funding.googleplay.ui.contribution.ContributionContract.ContributionListState
import net.thunderbird.feature.funding.googleplay.ui.contribution.ContributionContract.ContributionType

@Composable
internal fun ContributionList(
    state: ContributionListState,
    onContributionTypeClick: (ContributionType) -> Unit,
    onItemClick: (Contribution) -> Unit,
    onRetryClick: () -> Unit,
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

            ContentLoadingErrorView(
                state = state,
                loading = {
                    LoadingView()
                },
                error = { error ->
                    ListErrorView(
                        error = error,
                        onRetryClick = onRetryClick,
                    )
                },
                content = { state ->
                    if (state.oneTimeContributions.isEmpty() && state.recurringContributions.isEmpty()) {
                        ListEmptyView()
                    } else {
                        ListContentView(
                            state = state,
                            onContributionTypeClick = onContributionTypeClick,
                            onItemClick = onItemClick,
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
                modifier = Modifier.padding(top = MainTheme.spacings.default),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChoicesRow(
    contributions: ImmutableList<Contribution>,
    onItemClick: (Contribution) -> Unit,
    selectedItemId: ContributionId?,
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
                isSelected = it.id == selectedItemId,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ListContentView(
    state: ContributionListState,
    onContributionTypeClick: (ContributionType) -> Unit,
    onItemClick: (Contribution) -> Unit,
    modifier: Modifier = Modifier,
) {
    val oneTimeLabel = stringResource(R.string.funding_googleplay_contribution_list_type_one_time)
    val recurringLabel = stringResource(R.string.funding_googleplay_contribution_list_type_recurring)

    Column(
        verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
        modifier = modifier,
    ) {
        ButtonSegmentedSingleChoice(
            options = state.contributionTypes,
            selectedOption = if (state.isRecurringContributionSelected) {
                ContributionType.Recurring
            } else {
                ContributionType.OneTime
            },
            onClick = onContributionTypeClick,
            optionTitle = { type ->
                when (type) {
                    ContributionType.OneTime -> oneTimeLabel
                    ContributionType.Recurring -> recurringLabel
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        ChoicesRow(
            contributions = if (state.isRecurringContributionSelected) {
                state.recurringContributions
            } else {
                state.oneTimeContributions
            },
            selectedItemId = state.selectedContributionId,
            onItemClick = onItemClick,
        )
    }
}

@Composable
private fun ListEmptyView(
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.double),
        modifier = modifier.padding(vertical = MainTheme.spacings.double),
    ) {
        val annotatedString = annotatedStringResource(
            id = R.string.funding_googleplay_contribution_list_empty_message,
            argument = buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        color = MainTheme.colors.primary,
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
        verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.half),
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
                        .padding(MainTheme.spacings.quarter),
                )
            }

            AnimatedVisibility(visible = showDetails.value) {
                TextBodySmall(
                    text = error.message,
                    color = MainTheme.colors.onErrorContainer,
                )
            }
        }

        ButtonText(
            text = stringResource(R.string.funding_googleplay_contribution_list_error_retry_button),
            onClick = onRetryClick,
            modifier = Modifier.padding(top = MainTheme.spacings.default),
        )
    }
}
