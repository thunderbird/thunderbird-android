package net.thunderbird.core.ui.compose.preference.ui.components.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.theme2.MainTheme

@Composable
internal fun ColorView(
    color: Int,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = CircleShape,
        color = Color(color),
        modifier = modifier
            .size(MainTheme.sizes.icon)
            .also { modifier ->
                onClick?.let {
                    modifier.clickable(onClick = onClick)
                }
            }
    ) { }
}
