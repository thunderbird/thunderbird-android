package app.k9mail.core.ui.compose.designsystem.template

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.common.annotation.PreviewDevices
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonIcon
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons

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
