package net.thunderbird.feature.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun OnboardingScreen(
    onStartClick: () -> Unit,
    onImportClick: () -> Unit,
) {
    OnboardingContent(
        onStartClick = onStartClick,
        onImportClick = onImportClick,
    )
}

@Preview
@Composable
internal fun OnboardingScreenPreview() {
    OnboardingScreen(
        onStartClick = {},
        onImportClick = {},
    )
}
