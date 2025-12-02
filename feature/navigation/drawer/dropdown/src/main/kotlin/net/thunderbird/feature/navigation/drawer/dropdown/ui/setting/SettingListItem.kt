package net.thunderbird.feature.navigation.drawer.dropdown.ui.setting

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import app.k9mail.core.ui.compose.designsystem.organism.drawer.NavigationDrawerItem
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon

@Composable
internal fun SettingListItem(
    label: String,
    onClick: () -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
) {
    val rotation: Float = if (isLoading) {
        val infinite = rememberInfiniteTransition(label = "SyncIconRotation")
        val angle by infinite.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "SyncIconAngle",
        )
        angle
    } else {
        0f
    }

    NavigationDrawerItem(
        label = label,
        onClick = onClick,
        modifier = modifier,
        selected = false,
        icon = {
            Icon(
                imageVector = icon,
                modifier = if (rotation != 0f) Modifier.rotate(rotation) else Modifier,
            )
        },
    )
}
