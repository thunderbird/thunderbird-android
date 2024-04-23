package app.k9mail.core.ui.compose.designsystem.organism.drawer

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
