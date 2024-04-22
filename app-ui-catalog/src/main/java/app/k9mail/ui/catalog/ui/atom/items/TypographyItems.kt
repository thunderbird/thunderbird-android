package app.k9mail.ui.catalog.ui.atom.items

import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import app.k9mail.core.ui.compose.designsystem.atom.text.TextButton
import app.k9mail.core.ui.compose.designsystem.atom.text.TextHeadline1
import app.k9mail.core.ui.compose.designsystem.atom.text.TextHeadline3
import app.k9mail.ui.catalog.ui.common.list.itemDefaultPadding
import app.k9mail.ui.catalog.ui.common.list.sectionHeaderItem

fun LazyGridScope.typographyItems() {
    sectionHeaderItem(text = "Text styles")
    textItems()

    sectionHeaderItem(text = "Text styles - Colored")
    textItems(color = Color.Magenta)

    sectionHeaderItem(text = "Text styles - Annotated")
    textItems(isAnnotated = true)
}

@Suppress("LongMethod")
private fun LazyGridScope.textItems(
    isAnnotated: Boolean = false,
    color: Color = Color.Unspecified,
) {
    item {
        TextHeadline1(
            text = annotatedString("Headline1", isAnnotated),
            modifier = Modifier.itemDefaultPadding(),
            color = color,
        )
    }
    item {
        TextHeadline3(
            text = annotatedString("Headline3", isAnnotated),
            modifier = Modifier.itemDefaultPadding(),
            color = color,
        )
    }
    item {
        TextButton(
            text = annotatedString("Button", isAnnotated),
            modifier = Modifier.itemDefaultPadding(),
            color = color,
        )
    }
}

private fun annotatedString(
    name: String,
    isAnnotated: Boolean,
) = buildAnnotatedString {
    append(name)
    if (isAnnotated) {
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append("Annotated")
        }
    }
}
