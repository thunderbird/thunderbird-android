package app.k9mail.feature.funding.googleplay.ui.contribution

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.common.annotation.PreviewDevicesWithBackground
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import app.k9mail.feature.funding.googleplay.ui.contribution.ContributionContract.State

@Composable
@PreviewDevicesWithBackground
fun ContributionContentPreview() {
    PreviewWithTheme {
        ContributionContent(
            state = State(
                recurringContributions = FakeData.recurringContributions,
                oneTimeContributions = FakeData.oneTimeContributions,
                selectedContribution = FakeData.recurringContributions.first(),
            ),
            onEvent = {},
            contentPadding = PaddingValues(),
        )
    }
}

@Composable
@Preview(showBackground = true)
fun ContributionContentEmptyPreview() {
    PreviewWithTheme {
        ContributionContent(
            state = State(),
            onEvent = {},
            contentPadding = PaddingValues(),
        )
    }
}
