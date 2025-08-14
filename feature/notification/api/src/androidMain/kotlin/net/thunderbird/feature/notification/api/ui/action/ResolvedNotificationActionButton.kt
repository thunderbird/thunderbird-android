package net.thunderbird.feature.notification.api.ui.action

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.molecule.notification.NotificationActionButton
import kotlin.let

@Composable
internal fun ResolvedNotificationActionButton(
    action: NotificationAction,
    onActionClick: (NotificationAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember(action) { mutableStateOf<String?>(value = null) }

    LaunchedEffect(action) {
        text = action.resolveTitle()
    }

    text?.let { text ->
        NotificationActionButton(
            text = text,
            onClick = {
                onActionClick(action)
            },
            modifier = modifier,
        )
    }
}
