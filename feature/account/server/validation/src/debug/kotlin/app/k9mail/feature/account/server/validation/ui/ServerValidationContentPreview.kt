package app.k9mail.feature.account.server.validation.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import app.k9mail.feature.account.oauth.ui.fake.FakeAccountOAuthViewModel

@Composable
@Preview(showBackground = true)
internal fun IncomingServerValidationContentPreview() {
    PreviewWithTheme {
        ServerValidationContent(
            onEvent = { },
            state = ServerValidationContract.State(),
            isIncomingValidation = true,
            oAuthViewModel = viewModel { FakeAccountOAuthViewModel() },
            contentPadding = PaddingValues(),
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun OutgoingServerValidationContentPreview() {
    PreviewWithTheme {
        ServerValidationContent(
            onEvent = { },
            state = ServerValidationContract.State(),
            isIncomingValidation = false,
            oAuthViewModel = viewModel { FakeAccountOAuthViewModel() },
            contentPadding = PaddingValues(),
        )
    }
}
