package app.k9mail.ui.catalog.items

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.atom.text.TextSubtitle1
import app.k9mail.core.ui.compose.designsystem.molecule.EmailAddressInput
import app.k9mail.core.ui.compose.designsystem.molecule.ErrorView
import app.k9mail.core.ui.compose.designsystem.molecule.LoadingView
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.ui.catalog.helper.WithRememberedState

fun LazyGridScope.moleculeItems() {
    sectionHeaderItem(text = "Molecules")
    item {
        MoleculeWrapper(title = "ErrorView") {
            ErrorView(
                title = "Error",
                message = "Something went wrong",
            )
        }
    }

    item {
        MoleculeWrapper(title = "LoadingView") {
            LoadingView()
        }
    }
    item {
        MoleculeWrapper(title = "LoadingView with message") {
            LoadingView(
                message = "Loading...",
            )
        }
    }

    item {
        MoleculeWrapper(title = "EmailAddressInput") {
            WithRememberedState(input = "") { state ->
                EmailAddressInput(
                    emailAddress = state.value,
                    onEmailAddressChange = { state.value = it },
                )
            }
        }
    }
    item {
        MoleculeWrapper(title = "EmailAddressInput with error") {
            WithRememberedState(input = "wrong email address") { state ->
                EmailAddressInput(
                    emailAddress = state.value,
                    onEmailAddressChange = { state.value = it },
                    errorMessage = "Invalid email address",
                )
            }
        }
    }
}

@Composable
private fun MoleculeWrapper(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
    ) {
        TextSubtitle1(text = title)
        Box(
            modifier = Modifier
                .border(1.dp, Color.Gray)
                .fillMaxWidth(),
        ) {
            content()
        }
    }
}
