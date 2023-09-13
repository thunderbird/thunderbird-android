package app.k9mail.feature.account.setup.ui.autodiscovery.item

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import app.k9mail.core.common.domain.usecase.validation.ValidationError
import app.k9mail.core.ui.compose.designsystem.molecule.input.PasswordInput
import app.k9mail.feature.account.common.ui.item.ListItem
import app.k9mail.feature.account.server.settings.ui.common.mapper.toResourceString

@Composable
internal fun LazyItemScope.PasswordItem(
    password: String,
    error: ValidationError?,
    onPasswordChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val resources = LocalContext.current.resources

    ListItem(
        modifier = modifier,
    ) {
        PasswordInput(
            password = password,
            errorMessage = error?.toResourceString(resources),
            onPasswordChange = onPasswordChange,
            contentPadding = PaddingValues(),
        )
    }
}
