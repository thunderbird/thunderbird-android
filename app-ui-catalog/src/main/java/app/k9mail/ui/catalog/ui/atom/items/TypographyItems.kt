package app.k9mail.ui.catalog.ui.atom.items

import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyLarge
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodySmall
import app.k9mail.core.ui.compose.designsystem.atom.text.TextDisplayLarge
import app.k9mail.core.ui.compose.designsystem.atom.text.TextDisplayMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextDisplaySmall
import app.k9mail.core.ui.compose.designsystem.atom.text.TextHeadlineLarge
import app.k9mail.core.ui.compose.designsystem.atom.text.TextHeadlineMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextHeadlineSmall
import app.k9mail.core.ui.compose.designsystem.atom.text.TextLabelLarge
import app.k9mail.core.ui.compose.designsystem.atom.text.TextLabelMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextLabelSmall
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleLarge
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleSmall
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
        TextDisplayLarge(
            text = annotatedString("DisplayLarge", isAnnotated),
            modifier = Modifier.itemDefaultPadding(),
            color = color,
        )
    }
    item {
        TextDisplayMedium(
            text = annotatedString("DisplayMedium", isAnnotated),
            modifier = Modifier.itemDefaultPadding(),
            color = color,
        )
    }
    item {
        TextDisplaySmall(
            text = annotatedString("DisplaySmall", isAnnotated),
            modifier = Modifier.itemDefaultPadding(),
            color = color,
        )
    }

    item {
        TextHeadlineLarge(
            text = annotatedString("HeadlineLarge", isAnnotated),
            modifier = Modifier.itemDefaultPadding(),
            color = color,
        )
    }
    item {
        TextHeadlineMedium(
            text = annotatedString("HeadlineMedium", isAnnotated),
            modifier = Modifier.itemDefaultPadding(),
            color = color,
        )
    }
    item {
        TextHeadlineSmall(
            text = annotatedString("HeadlineSmall", isAnnotated),
            modifier = Modifier.itemDefaultPadding(),
            color = color,
        )
    }

    item {
        TextTitleLarge(
            text = annotatedString("TitleLarge", isAnnotated),
            modifier = Modifier.itemDefaultPadding(),
            color = color,
        )
    }
    item {
        TextTitleMedium(
            text = annotatedString("TitleMedium", isAnnotated),
            modifier = Modifier.itemDefaultPadding(),
            color = color,
        )
    }
    item {
        TextTitleSmall(
            text = annotatedString("TitleSmall", isAnnotated),
            modifier = Modifier.itemDefaultPadding(),
            color = color,
        )
    }

    item {
        TextBodyLarge(
            text = annotatedString("BodyLarge", isAnnotated),
            modifier = Modifier.itemDefaultPadding(),
            color = color,
        )
    }
    item {
        TextBodyMedium(
            text = annotatedString("BodyMedium", isAnnotated),
            modifier = Modifier.itemDefaultPadding(),
            color = color,
        )
    }
    item {
        TextBodySmall(
            text = annotatedString("BodySmall", isAnnotated),
            modifier = Modifier.itemDefaultPadding(),
            color = color,
        )
    }

    item {
        TextLabelLarge(
            text = annotatedString("LabelLarge", isAnnotated),
            modifier = Modifier.itemDefaultPadding(),
            color = color,
        )
    }
    item {
        TextLabelMedium(
            text = annotatedString("LabelMedium", isAnnotated),
            modifier = Modifier.itemDefaultPadding(),
            color = color,
        )
    }
    item {
        TextLabelSmall(
            text = annotatedString("LabelSmall", isAnnotated),
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
