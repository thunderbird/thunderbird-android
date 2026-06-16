package net.thunderbird.components.ui.bolt.template

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithTheme
import net.thunderbird.components.ui.bolt.atom.Surface
import net.thunderbird.components.ui.bolt.atom.button.ButtonIcon
import net.thunderbird.components.ui.bolt.common.annotation.PreviewDevices
import net.thunderbird.components.ui.bolt.atom.icon.Icons
import net.thunderbird.components.ui.bolt.theme.MainTheme

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
