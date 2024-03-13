package app.k9mail.core.ui.compose.designsystem.organism

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBody2
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.core.ui.compose.theme.PreviewWithThemes
import androidx.compose.material.TopAppBar as MaterialTopAppBar

@Composable
fun TopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    titleContentPadding: PaddingValues? = null,
) {
    MaterialTopAppBar(
        title = { TopAppBarTitle(title, subtitle, titleContentPadding) },
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        backgroundColor = MainTheme.colors.toolbar,
    )
}

@Composable
private fun TopAppBarTitle(
    title: String,
    subtitle: String?,
    titleContentPadding: PaddingValues?,
) {
    Column(
        if (titleContentPadding != null) {
            Modifier.padding(titleContentPadding)
        } else {
            Modifier
        },
    ) {
        Text(text = title)
        if (subtitle != null) {
            TextBody2(text = subtitle)
        }
    }
}

@Preview
@Composable
internal fun TopAppBarPreview() {
    PreviewWithThemes {
        TopAppBar(
            title = "Title",
            navigationIcon = {
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Outlined.menu,
                        contentDescription = null,
                    )
                }
            },
            actions = {
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Outlined.menu,
                        contentDescription = null,
                    )
                }
            },
        )
    }
}

@Preview
@Composable
internal fun TopAppBarWithSubtitlePreview() {
    PreviewWithThemes {
        TopAppBar(
            title = "Title",
            subtitle = "Subtitle",
        )
    }
}
