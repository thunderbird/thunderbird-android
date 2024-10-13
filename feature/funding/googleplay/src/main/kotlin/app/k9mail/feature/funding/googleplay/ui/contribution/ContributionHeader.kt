package app.k9mail.feature.funding.googleplay.ui.contribution

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.designsystem.atom.image.FixedScaleImage
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextHeadlineSmall
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.feature.funding.googleplay.R
import app.k9mail.feature.funding.googleplay.ui.contribution.image.HearthSunburst

@Composable
internal fun ContributionHeader(
    modifier: Modifier = Modifier,
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
                imageVector = HearthSunburst,
                contentDescription = null,
                alignment = Alignment.TopCenter,
                allowOverflow = true,
            )
        }

        TextHeadlineSmall(
            text = stringResource(R.string.funding_googleplay_contribution_header_title),
            color = MainTheme.colors.primary,
        )

        TextBodyMedium(
            text = stringResource(R.string.funding_googleplay_contribution_header_description),
        )
    }
}
