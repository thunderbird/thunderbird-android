package net.thunderbird.components.ui.bolt.organism

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithTheme
import net.thunderbird.components.ui.bolt.atom.text.TextBodyMedium
import net.thunderbird.components.ui.bolt.atom.icon.Icons
import net.thunderbird.components.ui.bolt.theme.MainTheme

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
