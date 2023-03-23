@file:Suppress("TooManyFunctions", "LongParameterList")

package app.k9mail.core.ui.compose.designsystem.organism

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.common.LightDarkPreviews
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonText
import app.k9mail.core.ui.compose.designsystem.atom.icon.IconError
import app.k9mail.core.ui.compose.designsystem.atom.icon.IconInfo
import app.k9mail.core.ui.compose.designsystem.atom.icon.IconSuccess
import app.k9mail.core.ui.compose.designsystem.atom.icon.IconWarning
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBody2
import app.k9mail.core.ui.compose.designsystem.atom.text.TextSubtitle1
import app.k9mail.core.ui.compose.theme.K9Theme
import app.k9mail.core.ui.compose.theme.MainTheme
import androidx.compose.material.AlertDialog as MaterialAlertDialog

@Composable
fun AlertDialog(
    title: String,
    text: String,
    confirmButtonText: String,
    onConfirmButtonClick: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    type: AlertDialogType = AlertDialogType.Info,
    hasTitleIcon: Boolean = false,
    dismissButtonText: String? = null,
    onDismissButtonClick: () -> Unit = {},
) {
    MaterialAlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = configureConfirmButton(confirmButtonText, onConfirmButtonClick),
        modifier = modifier,
        dismissButton = configureDismissButton(dismissButtonText, onDismissButtonClick),
        title = configureTitle(title, type, hasTitleIcon),
        text = configureText(text),
    )
}

private fun configureTitle(title: String?, type: AlertDialogType, hasIcon: Boolean): @Composable (() -> Unit)? {
    return if (title != null) {
        {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
            ) {
                ConfigureTitleIcon(hasIcon, type)
                TextSubtitle1(text = title)
            }
        }
    } else {
        null
    }
}

@Composable
private fun ConfigureTitleIcon(
    hasIcon: Boolean,
    type: AlertDialogType,
) {
    if (hasIcon) {
        val modifier = Modifier.padding(end = MainTheme.spacings.default)
        when (type) {
            AlertDialogType.Success -> IconSuccess(modifier = modifier)
            AlertDialogType.Error -> IconError(modifier = modifier)
            AlertDialogType.Warning -> IconWarning(modifier = modifier)
            AlertDialogType.Info -> IconInfo(modifier = modifier)
        }
    }
}

private fun configureText(text: String?): @Composable (() -> Unit)? {
    return if (text != null) {
        {
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                TextBody2(text = text)
            }
        }
    } else {
        null
    }
}

private fun configureConfirmButton(
    text: String,
    onClick: () -> Unit,
): @Composable (() -> Unit) {
    return {
        ButtonText(
            text = text,
            onClick = onClick,
        )
    }
}

private fun configureDismissButton(
    text: String?,
    onClick: () -> Unit,
): @Composable (() -> Unit)? {
    return if (text != null) {
        {
            ButtonText(
                text = text,
                onClick = onClick,
            )
        }
    } else {
        null
    }
}

enum class AlertDialogType {
    Error,
    Warning,
    Info,
    Success,
}

@Composable
@LightDarkPreviews
internal fun AlertDialogPreview() {
    K9Theme {
        AlertDialog(
            title = "Title",
            text = "Example text",
            confirmButtonText = "Confirm",
            onConfirmButtonClick = {},
            onDismissRequest = {},
            dismissButtonText = "Dismiss",
        )
    }
}

@Composable
@LightDarkPreviews
internal fun AlertDialogNoDismissPreview() {
    K9Theme {
        AlertDialog(
            title = "Title",
            text = "Example text",
            confirmButtonText = "Confirm",
            onConfirmButtonClick = {},
            onDismissRequest = {},
        )
    }
}

@Composable
@LightDarkPreviews
internal fun AlertDialogWithSuccessIconPreview() {
    K9Theme {
        AlertDialog(
            title = "Success title",
            text = "Success text",
            confirmButtonText = "Confirm",
            onConfirmButtonClick = {},
            onDismissRequest = {},
            dismissButtonText = "Dismiss",
            hasTitleIcon = true,
            type = AlertDialogType.Success,
        )
    }
}

@Composable
@LightDarkPreviews
internal fun AlertDialogWithErrorIconPreview() {
    K9Theme {
        AlertDialog(
            title = "Error title",
            text = "Error text",
            confirmButtonText = "Confirm",
            onConfirmButtonClick = {},
            onDismissRequest = {},
            dismissButtonText = "Dismiss",
            hasTitleIcon = true,
            type = AlertDialogType.Error,
        )
    }
}

@Composable
@LightDarkPreviews
internal fun AlertDialogWithWarningIconPreview() {
    K9Theme {
        AlertDialog(
            title = "Warning title",
            text = "Warning text",
            confirmButtonText = "Confirm",
            onConfirmButtonClick = {},
            onDismissRequest = {},
            dismissButtonText = "Dismiss",
            hasTitleIcon = true,
            type = AlertDialogType.Warning,
        )
    }
}

@Composable
@LightDarkPreviews
internal fun AlertDialogWithInfoIconPreview() {
    K9Theme {
        AlertDialog(
            title = "Info title",
            text = "Info text",
            confirmButtonText = "Confirm",
            onConfirmButtonClick = {},
            onDismissRequest = {},
            dismissButtonText = "Dismiss",
            hasTitleIcon = true,
            type = AlertDialogType.Info,
        )
    }
}
