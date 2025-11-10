package net.thunderbird.feature.navigation.drawer.dropdown.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon

@Composable
internal fun AnimatedExpandIcon(
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
    tint: Color? = null,
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "rotationAngle",
    )

    Icon(
        imageVector = Icons.Outlined.KeyboardArrowDown,
        contentDescription = null,
        tint = tint,
        modifier = modifier
            .rotate(rotationAngle),
    )
}
