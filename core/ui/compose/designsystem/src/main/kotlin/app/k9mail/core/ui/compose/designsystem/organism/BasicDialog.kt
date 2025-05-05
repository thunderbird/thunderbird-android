package app.k9mail.core.ui.compose.designsystem.organism

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import app.k9mail.core.ui.compose.designsystem.PreviewLightDarkLandscape
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemeLightDark
import app.k9mail.core.ui.compose.designsystem.atom.DividerHorizontal
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextHeadlineSmall
import app.k9mail.core.ui.compose.theme2.MainTheme
import androidx.compose.ui.window.Dialog as MaterialDialog

@Composable
fun BasicDialog(
    onDismissRequest: () -> Unit,
    content: (@Composable () -> Unit)?,
    buttons: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    headline: (@Composable ColumnScope.() -> Unit)? = null,
    supportingText: (@Composable ColumnScope.() -> Unit)? = null,
    contentPadding: PaddingValues = BasicDialogDefaults.contentPadding,
    showDividers: Boolean = BasicDialogDefaults.showDividers,
    dividerColor: Color = BasicDialogDefaults.dividerColor,
    properties: DialogProperties = DialogProperties(),
) {
    MaterialDialog(
        onDismissRequest = onDismissRequest,
        properties = properties,
    ) {
        BasicDialogContent(
            content = content,
            buttons = buttons,
            modifier = modifier,
            headline = headline,
            supportingText = supportingText,
            contentPadding = contentPadding,
            showDividers = showDividers,
            dividerColor = dividerColor,
        )
    }
}

@Composable
fun BasicDialog(
    onDismissRequest: () -> Unit,
    content: (@Composable () -> Unit)?,
    buttons: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    headlineText: String,
    supportingText: String?,
    contentPadding: PaddingValues = BasicDialogDefaults.contentPadding,
    showDividers: Boolean = BasicDialogDefaults.showDividers,
    dividerColor: Color = BasicDialogDefaults.dividerColor,
    properties: DialogProperties = DialogProperties(),
) {
    BasicDialog(
        onDismissRequest = onDismissRequest,
        content = content,
        buttons = buttons,
        modifier = modifier,
        headline = { TextHeadlineSmall(text = headlineText) },
        supportingText = supportingText?.let {
            @Composable {
                TextBodyMedium(
                    text = supportingText,
                    color = MainTheme.colors.onSurfaceVariant,
                )
            }
        },
        contentPadding = contentPadding,
        showDividers = showDividers,
        dividerColor = dividerColor,
        properties = properties,
    )
}

@Composable
private fun BasicDialogContent(
    content: (@Composable () -> Unit)?,
    buttons: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    headline: (@Composable ColumnScope.() -> Unit)? = null,
    supportingText: (@Composable ColumnScope.() -> Unit)? = null,
    contentPadding: PaddingValues = BasicDialogDefaults.contentPadding,
    showDividers: Boolean = BasicDialogDefaults.showDividers,
    dividerColor: Color = BasicDialogDefaults.dividerColor,
) {
    Surface(
        modifier = modifier,
        shape = MainTheme.shapes.extraLarge,
    ) {
        Column {
            Column(
                verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.double),
                modifier = Modifier
                    .padding(
                        start = MainTheme.spacings.triple,
                        end = MainTheme.spacings.triple,
                        top = MainTheme.spacings.triple,
                        bottom = MainTheme.spacings.double,
                    ),
            ) {
                headline?.invoke(this)
                supportingText?.invoke(this)
            }
            if (showDividers && (headline != null || supportingText != null)) {
                DividerHorizontal(
                    color = dividerColor,
                    modifier = Modifier.wrapContentSize(),
                )
            }
            content?.let { content ->
                Box(
                    modifier = Modifier
                        .weight(weight = 1f, fill = false)
                        .padding(contentPadding),
                ) {
                    content()
                }
            }
            if (showDividers && content != null) {
                DividerHorizontal(
                    color = dividerColor,
                    modifier = Modifier.wrapContentSize(),
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(
                        start = MainTheme.spacings.triple,
                        end = MainTheme.spacings.triple,
                        bottom = MainTheme.spacings.triple,
                    ),
            ) {
                Row { buttons() }
            }
        }
    }
}

object BasicDialogDefaults {
    val showDividers: Boolean get() = false
    val dividerColor: Color
        @Composable
        get() = MainTheme.colors.outlineVariant
    val contentPadding: PaddingValues
        @Composable
        get() = PaddingValues(
            top = MainTheme.spacings.oneHalf,
            bottom = MainTheme.spacings.double,
        )
}

@PreviewLightDarkLandscape
@Composable
private fun Preview() {
    PreviewWithThemeLightDark(
        useRow = true,
        useScrim = true,
        scrimPadding = PaddingValues(32.dp),
        arrangement = Arrangement.spacedBy(24.dp),
    ) {
        BasicDialogContent(
            headline = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.double),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                    TextHeadlineSmall(text = "Reset settings?")
                }
            },
            supportingText = {
                TextBodyMedium(
                    text = "This will reset your app preferences back to their default settings. " +
                        "The following accounts will also be signed out:",
                    color = MainTheme.colors.onSurfaceVariant,
                )
            },
            content = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.double),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = MainTheme.spacings.triple,
                            end = MainTheme.spacings.triple,
                        ),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.double),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(color = MainTheme.colors.primary, shape = CircleShape),
                        )
                        Text(text = "Account 1")
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.double),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(color = MainTheme.colors.primary, shape = CircleShape),
                        )
                        Text(text = "Account 2")
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.double),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(color = MainTheme.colors.primary, shape = CircleShape),
                        )
                        Text(text = "Account 3")
                    }
                }
            },
            buttons = {
                TextButton(onClick = {}) {
                    Text(text = "Cancel")
                }
                TextButton(onClick = {}) {
                    Text(text = "Accept")
                }
            },
            showDividers = true,
            modifier = Modifier.width(300.dp),
        )
    }
}

@PreviewLightDarkLandscape
@Composable
private fun PreviewOnlySupportingText() {
    PreviewWithThemeLightDark(
        useRow = true,
        useScrim = true,
        scrimPadding = PaddingValues(32.dp),
        arrangement = Arrangement.spacedBy(24.dp),
    ) {
        BasicDialogContent(
            headline = {
                TextHeadlineSmall(text = "Email can not be archived")
            },
            supportingText = {
                TextBodyMedium(
                    text = "Configure archive folder now",
                    color = MainTheme.colors.onSurfaceVariant,
                )
            },
            content = null,
            buttons = {
                TextButton(onClick = {}) {
                    Text(text = "Skip for now")
                }
                TextButton(onClick = {}) {
                    Text(text = "Set archive folder")
                }
            },
            showDividers = false,
            modifier = Modifier.width(300.dp),
        )
    }
}
