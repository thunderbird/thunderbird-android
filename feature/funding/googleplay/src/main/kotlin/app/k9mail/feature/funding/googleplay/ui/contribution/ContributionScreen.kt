package app.k9mail.feature.funding.googleplay.ui.contribution

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleLarge

@Composable
internal fun ContributionScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler {
        onBack()
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        TextTitleLarge(text = "ContributionScreen")
    }
}
