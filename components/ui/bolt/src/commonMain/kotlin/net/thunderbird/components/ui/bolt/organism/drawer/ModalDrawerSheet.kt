package net.thunderbird.components.ui.bolt.organism.drawer

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.ModalDrawerSheet as Material3ModalDrawerSheet

@Composable
fun ModalDrawerSheet(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Material3ModalDrawerSheet(
        modifier = modifier,
        content = content,
    )
}
