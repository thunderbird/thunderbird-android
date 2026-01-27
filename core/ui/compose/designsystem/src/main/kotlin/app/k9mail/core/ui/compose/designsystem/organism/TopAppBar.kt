package app.k9mail.core.ui.compose.designsystem.organism

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonIcon
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleLarge
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.core.ui.compose.common.modifier.testTagAsResourceId
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons
import androidx.compose.material3.TopAppBar as Material3TopAppBar

/**
 * A top app bar with a title, subtitle, navigation icon, and actions.
 *
 * @param title The title of the top app bar.
 * @param navigationIcon The icon to use for the navigation icon.
 * @param actions The actions to display in the top app bar.
 * @param modifier The modifier to apply to the top app bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable (() -> Unit) = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    Material3TopAppBar(
        title = { TextTitleLarge(text = title) },
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        colors = topAppBarColors(
            containerColor = MainTheme.colors.surfaceContainer,
        ),
    )
}

@Composable
fun TopAppBarWithMenuButton(
    title: String,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = title,
        modifier = modifier,
        navigationIcon = {
            ButtonIcon(
                onClick = onMenuClick,
                imageVector = Icons.Outlined.Menu,
                modifier = Modifier.testTagAsResourceId("TopAppBarMenuButton"),
            )
        },
        actions = actions,
    )
}

@Composable
fun TopAppBarWithBackButton(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = title,
        modifier = modifier,
        navigationIcon = {
            ButtonIcon(
                onClick = onBackClick,
                imageVector = Icons.Outlined.ArrowBack,
                modifier = Modifier.testTagAsResourceId("TopAppBarBackButton"),
            )
        },
        actions = actions,
    )
}
