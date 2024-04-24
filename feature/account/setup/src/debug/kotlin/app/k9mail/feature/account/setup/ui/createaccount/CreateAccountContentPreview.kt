package app.k9mail.feature.account.setup.ui.createaccount

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import app.k9mail.feature.account.setup.AccountSetupExternalContract.AccountCreator.AccountCreatorResult

@Composable
@Preview(showBackground = true)
internal fun CreateAccountContentSuccessPreview() {
    PreviewWithTheme {
        CreateAccountContent(
            state = CreateAccountContract.State(
                isLoading = false,
                error = null,
            ),
            contentPadding = PaddingValues(),
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun CreateAccountContentLoadingPreview() {
    PreviewWithTheme {
        CreateAccountContent(
            state = CreateAccountContract.State(
                isLoading = true,
                error = null,
            ),
            contentPadding = PaddingValues(),
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun CreateAccountContentErrorPreview() {
    PreviewWithTheme {
        CreateAccountContent(
            state = CreateAccountContract.State(
                isLoading = false,
                error = AccountCreatorResult.Error("Error message"),
            ),
            contentPadding = PaddingValues(),
        )
    }
}
