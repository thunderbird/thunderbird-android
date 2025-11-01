package app.k9mail.feature.account.server.settings.ui.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.molecule.input.PasswordInput
import app.k9mail.core.ui.compose.designsystem.molecule.input.inputContentPadding
import app.k9mail.feature.account.common.domain.entity.InteractionMode

@Composable
fun ServerSettingsPasswordInput(
    mode: InteractionMode,
    onPasswordChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    password: String = "",
    isRequired: Boolean = false,
    errorMessage: String? = null,
    contentPadding: PaddingValues = inputContentPadding(),
) {
    if (mode == InteractionMode.Create) {
        PasswordInput(
            onPasswordChange = onPasswordChange,
            modifier = modifier,
            password = password,
            isRequired = isRequired,
            errorMessage = errorMessage,
            contentPadding = contentPadding,
        )
    } else {
        ProtectedPasswordInput(
            onPasswordChange = onPasswordChange,
            modifier = modifier,
            password = password,
            isRequired = isRequired,
            errorMessage = errorMessage,
            contentPadding = contentPadding,
        )
    }
}
