package net.thunderbird.components.ui.bolt.template

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FabPosition as Material3FabPosition
import androidx.compose.material3.Scaffold as Material3Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithTheme
import net.thunderbird.components.ui.bolt.atom.Surface
import net.thunderbird.components.ui.bolt.atom.button.ButtonIcon
import net.thunderbird.components.ui.bolt.atom.icon.Icons
import net.thunderbird.components.ui.bolt.common.annotation.PreviewDevices
import net.thunderbird.components.ui.bolt.theme.MainTheme

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

@Composable
@PreviewDevices
internal fun ScaffoldPreview() {
    PreviewWithTheme {
        Scaffold(
            topBar = {
                Surface(
                    color = MainTheme.colors.error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(MainTheme.sizes.topBarHeight),
                ) {}
            },
            bottomBar = {
                Surface(
                    color = MainTheme.colors.warning,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(MainTheme.sizes.bottomBarHeight),
                ) {}
            },
        ) { contentPadding ->
            Surface(
                color = MainTheme.colors.info,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            ) {}
        }
    }
}

@Composable
@Preview(showBackground = true)
internal fun ScaffoldWitFabPreview() {
    PreviewWithTheme {
        Scaffold(
            topBar = {
                Surface(
                    color = MainTheme.colors.error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(MainTheme.sizes.topBarHeight),
                ) {}
            },
            bottomBar = {
                Surface(
                    color = MainTheme.colors.warning,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(MainTheme.sizes.bottomBarHeight),
                ) {}
            },
            floatingActionButton = {
                ButtonIcon(
                    onClick = { },
                    imageVector = Icons.Outlined.Check,
                )
            },
        ) { contentPadding ->
            Surface(
                color = MainTheme.colors.surface,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            ) {}
        }
    }
}
