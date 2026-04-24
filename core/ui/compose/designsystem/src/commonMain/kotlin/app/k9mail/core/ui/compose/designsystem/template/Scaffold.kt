package app.k9mail.core.ui.compose.designsystem.template

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.FabPosition as Material3FabPosition
import androidx.compose.material3.Scaffold as Material3Scaffold

@Suppress("LongParameterList")
@Composable
fun Scaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: ScaffoldFabPosition = ScaffoldFabPosition.End,
    content: @Composable (PaddingValues) -> Unit,
) {
    Material3Scaffold(
        modifier = modifier,
        topBar = topBar,
        bottomBar = bottomBar,
        snackbarHost = snackbarHost,
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition.toMaterialFabPosition(),
        content = content,
    )
}

enum class ScaffoldFabPosition {
    Start,
    Center,
    End,
    EndOverlay,
}

private fun ScaffoldFabPosition.toMaterialFabPosition(): Material3FabPosition {
    return when (this) {
        ScaffoldFabPosition.Start -> Material3FabPosition.Start
        ScaffoldFabPosition.Center -> Material3FabPosition.Center
        ScaffoldFabPosition.End -> Material3FabPosition.End
        ScaffoldFabPosition.EndOverlay -> Material3FabPosition.EndOverlay
    }
}
