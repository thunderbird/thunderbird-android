package app.k9mail.core.ui.compose.designsystem.molecule.input

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.textfield.TextFieldOutlinedPassword
import net.thunderbird.core.ui.designsystem.resources.Res
import net.thunderbird.core.ui.designsystem.resources.designsystem_molecule_password_input_label
import org.jetbrains.compose.resources.stringResource

@Composable
fun PasswordInput(
    onPasswordChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    password: String = "",
    isRequired: Boolean = false,
    errorMessage: String? = null,
    contentPadding: PaddingValues = inputContentPadding(),
) {
    InputLayout(
        modifier = modifier,
        contentPadding = contentPadding,
        errorMessage = errorMessage,
    ) {
        TextFieldOutlinedPassword(
            value = password,
            onValueChange = onPasswordChange,
            label = stringResource(Res.string.designsystem_molecule_password_input_label),
            isRequired = isRequired,
            hasError = errorMessage != null,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
