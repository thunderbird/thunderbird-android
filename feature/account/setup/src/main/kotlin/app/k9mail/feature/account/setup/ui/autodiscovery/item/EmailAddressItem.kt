package app.k9mail.feature.account.setup.ui.autodiscovery.item

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import app.k9mail.core.common.domain.usecase.validation.ValidationError
import app.k9mail.core.ui.compose.designsystem.molecule.input.EmailAddressInput
import app.k9mail.feature.account.common.ui.item.ListItem
import app.k9mail.feature.account.setup.ui.autodiscovery.toResourceString

@Composable
internal fun LazyItemScope.EmailAddressItem(
    emailAddress: String,
    error: ValidationError?,
    onEmailAddressChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
) {
    val resources = LocalContext.current.resources

    ListItem(
        modifier = modifier,
    ) {
        EmailAddressInput(
            emailAddress = emailAddress,
            errorMessage = error?.toResourceString(resources),
            onEmailAddressChange = onEmailAddressChange,
            isEnabled = isEnabled,
            contentPadding = PaddingValues(),
        )
    }
}
