package app.k9mail.core.ui.compose.designsystem.template

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.DrawerState
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.common.DevicePreviews
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.theme.K9Theme
import app.k9mail.core.ui.compose.theme.MainTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material.FabPosition as MaterialFabPosition
import androidx.compose.material.Scaffold as MaterialScaffold

@Suppress("LongParameterList")
@Composable
fun Scaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable (toggleDrawer: () -> Unit) -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: ScaffoldFabPosition = ScaffoldFabPosition.End,
    drawerContent: @Composable (ColumnScope.(toggleDrawer: () -> Unit) -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    val scaffoldState = rememberScaffoldState()
    val coroutineScope = rememberCoroutineScope()

    MaterialScaffold(
        modifier = modifier,
        scaffoldState = scaffoldState,
        topBar = {
            topBar { toggleDrawer(scaffoldState.drawerState, coroutineScope) }
        },
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition.toMaterialFabPosition(),
        drawerContent = drawerContent?.let { providedDrawerContent ->
            {
                providedDrawerContent { toggleDrawer(scaffoldState.drawerState, coroutineScope) }
            }
        },
        drawerGesturesEnabled = scaffoldState.drawerState.isOpen,
        content = content,
    )
}

private fun toggleDrawer(
    drawerState: DrawerState,
    coroutineScope: CoroutineScope,
) {
    coroutineScope.launch {
        delay(timeMillis = DRAWER_TOGGLE_DELAY)
        if (drawerState.isClosed) {
            drawerState.open()
        } else {
            drawerState.close()
        }
    }
}

enum class ScaffoldFabPosition {
    End,
    Center,
}

private fun ScaffoldFabPosition.toMaterialFabPosition(): MaterialFabPosition {
    return when (this) {
        ScaffoldFabPosition.End -> MaterialFabPosition.End
        ScaffoldFabPosition.Center -> MaterialFabPosition.Center
    }
}

/**
 * Delay before opening/closing the drawer to avoid the drawer being opened/closed
 * immediately and give time for the ripple effect to finish.
 */
private const val DRAWER_TOGGLE_DELAY = 250L

@Composable
@DevicePreviews
internal fun ScaffoldPreview() {
    K9Theme {
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
                    color = MainTheme.colors.error,
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
