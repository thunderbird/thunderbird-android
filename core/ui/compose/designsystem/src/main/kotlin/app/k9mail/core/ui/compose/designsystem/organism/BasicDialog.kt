package app.k9mail.core.ui.compose.designsystem.organism

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.DialogProperties
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
    headlineText: String,
    onDismissRequest: () -> Unit,
    content: (@Composable () -> Unit)?,
    buttons: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
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
internal fun BasicDialogContent(
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
