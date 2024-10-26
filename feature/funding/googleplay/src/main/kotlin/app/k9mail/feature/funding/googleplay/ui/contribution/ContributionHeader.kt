package app.k9mail.feature.funding.googleplay.ui.contribution

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icon
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons
import app.k9mail.core.ui.compose.designsystem.atom.image.FixedScaleImage
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextHeadlineSmall
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.feature.funding.googleplay.R
import app.k9mail.feature.funding.googleplay.domain.entity.Contribution
import app.k9mail.feature.funding.googleplay.ui.contribution.image.GoldenHearthSunburst
import app.k9mail.feature.funding.googleplay.ui.contribution.image.HearthSunburst
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun ContributionHeader(
    purchasedContribution: Contribution?,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = purchasedContribution != null,
        label = "ContributionHeaderLogo",
    ) { targetState ->
        when (targetState) {
            true -> {
                val contribution = purchasedContribution!!
                ContributionHeaderView(
                    logo = GoldenHearthSunburst,
                    title = ContributionIdStringMapper.mapToContributionTitle(contribution.id),
                    description = ContributionIdStringMapper.mapToContributionDescription(contribution.id),
                    thankYou = stringResource(R.string.funding_googleplay_contribution_header_thank_you_message),
                    benefits = ContributionIdStringMapper.mapToContributionBenefits(contribution.id),
                    modifier = modifier,
                )
            }

            false -> {
                ContributionHeaderView(
                    logo = HearthSunburst,
                    title = stringResource(R.string.funding_googleplay_contribution_header_title),
                    description = stringResource(R.string.funding_googleplay_contribution_header_description),
                    modifier = modifier,
                )
            }
        }
    }
}

@Composable
private fun ContributionHeaderView(
    logo: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    thankYou: String? = null,
    benefits: ImmutableList<String> = persistentListOf(),
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.triple),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = MainTheme.spacings.triple)
                .height(MainTheme.sizes.large),
        ) {
            FixedScaleImage(
                imageVector = logo,
                contentDescription = null,
                alignment = Alignment.TopCenter,
                allowOverflow = true,
            )
        }

        TextHeadlineSmall(
            text = title,
            color = MainTheme.colors.primary,
            textAlign = TextAlign.Center,
        )

        if (thankYou != null) {
            TextBodyMedium(
                text = thankYou,
            )
        }

        if (benefits.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
            ) {
                benefits.forEach { benefit ->
                    ContributionBenefit(
                        benefit = benefit,
                    )
                }
            }
        }

        TextBodyMedium(
            text = description,
        )
    }
}

@Composable
private fun ContributionBenefit(
    benefit: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.half),
    ) {
        Icon(
            imageVector = Icons.Filled.Dot,
            modifier = Modifier.size(MainTheme.sizes.small),
        )
        TextBodyMedium(
            text = benefit,
        )
    }
}
