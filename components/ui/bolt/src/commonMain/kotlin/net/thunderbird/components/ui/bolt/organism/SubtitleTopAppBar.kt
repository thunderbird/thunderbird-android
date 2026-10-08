package net.thunderbird.components.ui.bolt.organism

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar as Material3TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithThemes
import net.thunderbird.components.ui.bolt.atom.button.ButtonIcon
import net.thunderbird.components.ui.bolt.atom.icon.Icons
import net.thunderbird.components.ui.bolt.atom.text.TextBodyMedium
import net.thunderbird.components.ui.bolt.atom.text.TextTitleMedium
import net.thunderbird.components.ui.bolt.resources.Res
import net.thunderbird.components.ui.bolt.resources.bolt_organism_subtitle_top_app_bar_up_description
import net.thunderbird.components.ui.bolt.theme.BoltTheme
import org.jetbrains.compose.resources.stringResource

/**
 * A top app bar with a title, subtitle, navigation icon, and actions.
 *
 * @param title The title of the top app bar.
 * @param subtitle The subtitle of the top app bar (optional).
 * @param navigationIcon The icon to use for the navigation icon.
 * @param actions The actions to display in the top app bar.
 * @param modifier The modifier to apply to the top app bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtitleTopAppBar(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable (() -> Unit) = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    Material3TopAppBar(
        title = {
            Column(
                modifier = Modifier.padding(end = BoltTheme.spacings.double),
            ) {
                TextTitleMedium(text = title)
                TextBodyMedium(
                    text = subtitle,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
            }
        },
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        colors = topAppBarColors(
            containerColor = BoltTheme.colors.surfaceContainer,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtitleTopAppBarWithMenuButton(
    title: String,
    subtitle: String,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    SubtitleTopAppBar(
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        navigationIcon = {
            ButtonIcon(
                onClick = onMenuClick,
                imageVector = Icons.Outlined.Menu,
                modifier = Modifier.testTag("SubtitleTopAppBarMenuButton"),
            )
        },
        actions = actions,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtitleTopAppBarWithBackButton(
    title: String,
    subtitle: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    SubtitleTopAppBar(
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        navigationIcon = {
            ButtonIcon(
                onClick = onBackClick,
                imageVector = Icons.Outlined.ArrowBack,
                modifier = Modifier.testTag("SubtitleTopAppBarBackButton"),
                contentDescription = stringResource(
                    Res.string.bolt_organism_subtitle_top_app_bar_up_description,
                ),
            )
        },
        actions = actions,
    )
}

@Composable
@Preview(showBackground = true)
internal fun SubtitleTopAppBarPreview() {
    PreviewWithThemes {
        SubtitleTopAppBar(
            title = "Title",
            subtitle = "Subtitle",
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun SubtitleTopAppBarWithLongSubtitlePreview() {
    PreviewWithThemes {
        SubtitleTopAppBar(
            title = "Title",
            subtitle = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
                "Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun SubtitleTopAppBarWithActionsPreview() {
    PreviewWithThemes {
        SubtitleTopAppBar(
            title = "Title",
            subtitle = "Subtitle",
            actions = {
                ButtonIcon(
                    onClick = {},
                    imageVector = Icons.Outlined.Info,
                )
                ButtonIcon(
                    onClick = {},
                    imageVector = Icons.Outlined.Check,
                )
                ButtonIcon(
                    onClick = {},
                    imageVector = Icons.Outlined.Visibility,
                )
            },
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun SubtitleTopAppBarWithMenuButtonPreview() {
    PreviewWithThemes {
        SubtitleTopAppBarWithMenuButton(
            title = "Title",
            subtitle = "Subtitle",
            onMenuClick = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun SubtitleTopAppBarWithBackButtonPreview() {
    PreviewWithThemes {
        SubtitleTopAppBarWithBackButton(
            title = "Title",
            subtitle = "Subtitle",
            onBackClick = {},
        )
    }
}
