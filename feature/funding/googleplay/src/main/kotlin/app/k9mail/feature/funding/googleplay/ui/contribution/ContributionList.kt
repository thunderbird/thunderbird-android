package app.k9mail.feature.funding.googleplay.ui.contribution

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
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonText
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyLarge
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodySmall
import app.k9mail.core.ui.compose.designsystem.atom.text.TextLabelLarge
import app.k9mail.core.ui.compose.designsystem.molecule.ContentLoadingErrorView
import app.k9mail.core.ui.compose.designsystem.molecule.LoadingView
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.feature.funding.googleplay.R
import app.k9mail.feature.funding.googleplay.domain.DomainContract.BillingError
import app.k9mail.feature.funding.googleplay.domain.entity.Contribution
import app.k9mail.feature.funding.googleplay.domain.entity.OneTimeContribution
import app.k9mail.feature.funding.googleplay.domain.entity.RecurringContribution
import app.k9mail.feature.funding.googleplay.ui.contribution.ContributionContract.ContributionListState
import kotlinx.collections.immutable.ImmutableList
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon

@Composable
internal fun ContributionList(
    state: ContributionListState,
    onOneTimeContributionTypeClick: () -> Unit,
    onRecurringContributionTypeClick: () -> Unit,
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
                            onOneTimeContributionTypeClick = onOneTimeContributionTypeClick,
                            onRecurringContributionTypeClick = onRecurringContributionTypeClick,
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

@Composable
private fun ListContentView(
    state: ContributionListState,
    onOneTimeContributionTypeClick: () -> Unit,
    onRecurringContributionTypeClick: () -> Unit,
    onItemClick: (Contribution) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
        modifier = modifier,
    ) {
        TypeSelectionRow(
            oneTimeContributions = state.oneTimeContributions,
            recurringContributions = state.recurringContributions,
            isRecurringContributionSelected = state.isRecurringContributionSelected,
            onOneTimeContributionTypeClick = onOneTimeContributionTypeClick,
            onRecurringContributionTypeClick = onRecurringContributionTypeClick,
        )

        ChoicesRow(
            contributions = if (state.isRecurringContributionSelected) {
                state.recurringContributions
            } else {
                state.oneTimeContributions
            },
            selectedItem = state.selectedContribution,
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
    error: BillingError,
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
