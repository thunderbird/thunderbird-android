package net.thunderbird.components.ui.bolt.organism

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog as MaterialAlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithTheme
import net.thunderbird.components.ui.bolt.atom.button.ButtonText
import net.thunderbird.components.ui.bolt.atom.icon.Icon
import net.thunderbird.components.ui.bolt.atom.icon.Icons
import net.thunderbird.components.ui.bolt.atom.text.TextBodyMedium
import net.thunderbird.components.ui.bolt.atom.text.TextHeadlineSmall
import net.thunderbird.components.ui.bolt.theme.MainTheme

@Composable
fun AlertDialog(
    title: String,
    text: String,
    confirmText: String,
    onConfirmClick: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    dismissText: String? = null,
    onDismissClick: () -> Unit = {},
    confirmButtonEnabled: Boolean = true,
) {
    AlertDialog(
        title = title,
        icon = icon,
        confirmText = confirmText,
        onConfirmClick = onConfirmClick,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        dismissText = dismissText,
        onDismissClick = onDismissClick,
        confirmButtonEnabled = confirmButtonEnabled,
    ) {
        TextBodyMedium(text = text)
    }
}

@Composable
fun AlertDialog(
    title: String,
    confirmText: String,
    onConfirmClick: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    dismissText: String? = null,
    onDismissClick: () -> Unit = {},
    confirmButtonEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialAlertDialog(
        title = {
            TextHeadlineSmall(
                text = title,
                textAlign = if (icon == null) TextAlign.Start else TextAlign.Center,
            )
        },
        icon = icon?.let {
            {
                Icon(imageVector = it)
            }
        },
        text = { content() },
        confirmButton = {
            ButtonText(
                text = confirmText,
                onClick = onConfirmClick,
                enabled = confirmButtonEnabled,
            )
        },
        dismissButton = dismissText?.let {
            {
                ButtonText(
                    text = it,
                    onClick = onDismissClick,
                )
            }
        },
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    )
}

@Composable
@Preview(showBackground = true)
internal fun AlertDialogPreview() {
    PreviewWithTheme {
        AlertDialog(
            title = "Title",
            text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
                "Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
            confirmText = "Accept",
            onConfirmClick = {},
            onDismissRequest = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun AlertDialogWithIconPreview() {
    PreviewWithTheme {
        AlertDialog(
            icon = Icons.Outlined.Info,
            title = "Title",
            text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
                "Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
            confirmText = "Accept",
            onConfirmClick = {},
            onDismissRequest = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun AlertDialogWithCancelPreview() {
    PreviewWithTheme {
        AlertDialog(
            icon = Icons.Outlined.Info,
            title = "Title",
            text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
                "Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
            confirmText = "Accept",
            dismissText = "Cancel",
            onConfirmClick = {},
            onDismissRequest = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun AlertDialogWithCustomContentPreview() {
    PreviewWithTheme {
        AlertDialog(
            icon = Icons.Outlined.Info,
            title = "Title",
            confirmText = "Accept",
            dismissText = "Cancel",
            onConfirmClick = {},
            onDismissRequest = {},
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.double),
            ) {
                TextBodyMedium("Lorem ipsum dolor sit amet, consectetur adipiscing elit.")
                TextBodyMedium("Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.")
            }
        }
    }
}
