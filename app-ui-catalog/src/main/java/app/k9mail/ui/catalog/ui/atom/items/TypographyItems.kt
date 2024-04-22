package app.k9mail.ui.catalog.ui.atom.items

import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBody2
import app.k9mail.core.ui.compose.designsystem.atom.text.TextButton
import app.k9mail.core.ui.compose.designsystem.atom.text.TextCaption
import app.k9mail.core.ui.compose.designsystem.atom.text.TextHeadline1
import app.k9mail.core.ui.compose.designsystem.atom.text.TextHeadline2
import app.k9mail.core.ui.compose.designsystem.atom.text.TextHeadline3
import app.k9mail.core.ui.compose.designsystem.atom.text.TextHeadline4
import app.k9mail.core.ui.compose.designsystem.atom.text.TextHeadline5
import app.k9mail.core.ui.compose.designsystem.atom.text.TextHeadline6
import app.k9mail.core.ui.compose.designsystem.atom.text.TextOverline
import app.k9mail.core.ui.compose.designsystem.atom.text.TextSubtitle1
import app.k9mail.core.ui.compose.designsystem.atom.text.TextSubtitle2
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
        TextHeadline2(
            text = annotatedString("Headline2", isAnnotated),
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
        TextHeadline4(
            text = annotatedString("Headline4", isAnnotated),
            modifier = Modifier.itemDefaultPadding(),
            color = color,
        )
    }
    item {
        TextHeadline5(
            text = annotatedString("Headline5", isAnnotated),
            modifier = Modifier.itemDefaultPadding(),
            color = color,
        )
    }

    item {
        TextHeadline6(
            text = annotatedString("Headline6", isAnnotated),
            modifier = Modifier.itemDefaultPadding(),
            color = color,
        )
    }
    item {
        TextSubtitle1(
            text = annotatedString("Subtitle1", isAnnotated),
            modifier = Modifier.itemDefaultPadding(),
            color = color,
        )
    }
    item {
        TextSubtitle2(
            text = annotatedString("Subtitle2", isAnnotated),
            modifier = Modifier.itemDefaultPadding(),
            color = color,
        )
    }
    item {
        TextBody2(
            text = annotatedString("Body2", isAnnotated),
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
    item {
        TextCaption(
            text = annotatedString("Caption", isAnnotated),
            modifier = Modifier.itemDefaultPadding(),
            color = color,
        )
    }
    item {
        TextOverline(
            text = annotatedString("Overline", isAnnotated),
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
