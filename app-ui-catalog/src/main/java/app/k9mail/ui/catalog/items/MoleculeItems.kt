package app.k9mail.ui.catalog.items

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.molecule.ErrorView

fun LazyGridScope.moleculeItems() {
    sectionHeaderItem(text = "Molecules")
    item {
        MoleculeWrapper {
            ErrorView(
                title = "Error",
                message = "Something went wrong",
            )
        }
    }
}

@Composable
private fun MoleculeWrapper(
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier.border(1.dp, Color.Gray),
    ) {
        content()
    }
}
