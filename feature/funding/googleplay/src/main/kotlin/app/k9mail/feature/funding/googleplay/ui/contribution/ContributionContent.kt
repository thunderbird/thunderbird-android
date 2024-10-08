package app.k9mail.feature.funding.googleplay.ui.contribution

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleLarge
import app.k9mail.feature.funding.googleplay.ui.contribution.ContributionContract.Event
import app.k9mail.feature.funding.googleplay.ui.contribution.ContributionContract.State

@Composable
internal fun ContributionContent(
    state: State,
    onEvent: (Event) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize()
            .padding(contentPadding)
            .clickable(
                enabled = false,
                onClick = { onEvent(Event.OnPurchaseClicked) },
            ),
    ) {
        TextTitleLarge(
            text = "ContributionScreen",
        )

        TextBodyMedium(text = "is recurring selected: ${state.isRecurringContributionSelected}")
    }
}
