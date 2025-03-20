package net.thunderbird.core.ui.compose.preference.ui.components.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icon
import app.k9mail.core.ui.compose.theme2.MainTheme

@Composable
internal fun PreferenceItemLayout(
    onClick: () -> Unit,
    icon: ImageVector?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon?.let {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(MainTheme.spacings.double),
                ) {
                    Icon(
                        imageVector = it,
                    )
                }
            }
            Column(
                modifier = Modifier.padding(MainTheme.spacings.double),
                verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.half),
            ) {
                content()
            }
        }
    }
}
