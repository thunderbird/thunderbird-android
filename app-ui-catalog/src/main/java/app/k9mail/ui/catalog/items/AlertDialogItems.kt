package app.k9mail.ui.catalog.items

import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import app.k9mail.core.ui.compose.designsystem.atom.button.Button
import app.k9mail.core.ui.compose.designsystem.organism.AlertDialog
import app.k9mail.core.ui.compose.designsystem.organism.AlertDialogType

fun LazyGridScope.alertDialogItems() {
    sectionHeaderItem(text = "Alert dialogs")
    item {
        AlertDialogContent(
            openText = "Open default dialog",
            title = "Default alert dialog",
        )
    }
    item {
        WithOpenButton(
            "Open dialog with dismiss button",
        ) { state ->
            AlertDialog(
                title = "Dialog with dismiss button",
                text = "This is an dialog",
                confirmButtonText = "Confirm",
                onConfirmButtonClick = { state.value = false },
                onDismissRequest = { state.value = false },
                dismissButtonText = "Dismiss",
                onDismissButtonClick = { state.value = false },
            )
        }
    }
    item {
        AlertDialogContent(
            openText = "Open success dialog",
            title = "Success dialog",
            type = AlertDialogType.Success,
            hasTitleIcon = true,
        )
    }
    item {
        AlertDialogContent(
            openText = "Open error dialog",
            title = "Error dialog",
            type = AlertDialogType.Error,
            hasTitleIcon = true,
        )
    }
    item {
        AlertDialogContent(
            openText = "Open warning dialog",
            title = "Warning dialog",
            type = AlertDialogType.Warning,
            hasTitleIcon = true,
        )
    }
    item {
        AlertDialogContent(
            openText = "Open info dialog",
            title = "Info dialog",
            type = AlertDialogType.Info,
            hasTitleIcon = true,
        )
    }
}

@Composable
private fun AlertDialogContent(
    openText: String,
    title: String,
    type: AlertDialogType = AlertDialogType.Info,
    hasTitleIcon: Boolean = false,
) {
    WithOpenButton(
        text = openText,
    ) { state ->
        AlertDialog(
            title = title,
            text = "This is an dialog",
            confirmButtonText = "Confirm",
            onConfirmButtonClick = { state.value = false },
            onDismissRequest = { state.value = false },
            type = type,
            hasTitleIcon = hasTitleIcon,
        )
    }
}

@Composable
private fun WithOpenButton(
    text: String,
    content: @Composable (state: MutableState<Boolean>) -> Unit,
) {
    val dialogOpened = remember { mutableStateOf(false) }

    Button(text = text, onClick = { dialogOpened.value = true })

    if (dialogOpened.value) {
        content(dialogOpened)
    }
}
