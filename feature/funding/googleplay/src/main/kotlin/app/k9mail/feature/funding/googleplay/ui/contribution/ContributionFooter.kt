package app.k9mail.feature.funding.googleplay.ui.contribution

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonFilled
import app.k9mail.feature.funding.googleplay.R

@Composable
internal fun ContributionFooter(
    onClick: () -> Unit,
    isPurchaseEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
    ) {
        ButtonFilled(
            text = stringResource(
                R.string.funding_googleplay_contribution_footer_payment_button,
            ),
            onClick = onClick,
            enabled = isPurchaseEnabled,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
