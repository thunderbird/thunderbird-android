package net.thunderbird.components.ui.catalog.ui.page.atom.items

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import net.thunderbird.components.ui.bolt.atom.text.TextBodyLarge
import net.thunderbird.components.ui.bolt.atom.text.TextBodyMedium
import net.thunderbird.components.ui.bolt.atom.text.TextBodySmall
import net.thunderbird.components.ui.bolt.atom.text.TextDisplayLarge
import net.thunderbird.components.ui.bolt.atom.text.TextDisplayMedium
import net.thunderbird.components.ui.bolt.atom.text.TextDisplaySmall
import net.thunderbird.components.ui.bolt.atom.text.TextHeadlineLarge
import net.thunderbird.components.ui.bolt.atom.text.TextHeadlineMedium
import net.thunderbird.components.ui.bolt.atom.text.TextHeadlineSmall
import net.thunderbird.components.ui.bolt.atom.text.TextLabelLarge
import net.thunderbird.components.ui.bolt.atom.text.TextLabelMedium
import net.thunderbird.components.ui.bolt.atom.text.TextLabelSmall
import net.thunderbird.components.ui.bolt.atom.text.TextTitleLarge
import net.thunderbird.components.ui.bolt.atom.text.TextTitleMedium
import net.thunderbird.components.ui.bolt.atom.text.TextTitleSmall
import net.thunderbird.components.ui.catalog.ui.page.common.list.defaultItemPadding
import net.thunderbird.components.ui.catalog.ui.page.common.list.fullSpanItem
import net.thunderbird.components.ui.catalog.ui.page.common.list.sectionHeaderItem

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
    fullSpanItem {
        TextDisplayLarge(
            text = annotatedString("DisplayLarge", isAnnotated),
            modifier = Modifier.padding(defaultItemPadding()),
            color = color,
        )
    }
    fullSpanItem {
        TextDisplayMedium(
            text = annotatedString("DisplayMedium", isAnnotated),
            modifier = Modifier.padding(defaultItemPadding()),
            color = color,
        )
    }
    fullSpanItem {
        TextDisplaySmall(
            text = annotatedString("DisplaySmall", isAnnotated),
            modifier = Modifier.padding(defaultItemPadding()),
            color = color,
        )
    }

    fullSpanItem {
        TextHeadlineLarge(
            text = annotatedString("HeadlineLarge", isAnnotated),
            modifier = Modifier.padding(defaultItemPadding()),
            color = color,
        )
    }
    fullSpanItem {
        TextHeadlineMedium(
            text = annotatedString("HeadlineMedium", isAnnotated),
            modifier = Modifier.padding(defaultItemPadding()),
            color = color,
        )
    }
    fullSpanItem {
        TextHeadlineSmall(
            text = annotatedString("HeadlineSmall", isAnnotated),
            modifier = Modifier.padding(defaultItemPadding()),
            color = color,
        )
    }

    fullSpanItem {
        TextTitleLarge(
            text = annotatedString("TitleLarge", isAnnotated),
            modifier = Modifier.padding(defaultItemPadding()),
            color = color,
        )
    }
    fullSpanItem {
        TextTitleMedium(
            text = annotatedString("TitleMedium", isAnnotated),
            modifier = Modifier.padding(defaultItemPadding()),
            color = color,
        )
    }
    fullSpanItem {
        TextTitleSmall(
            text = annotatedString("TitleSmall", isAnnotated),
            modifier = Modifier.padding(defaultItemPadding()),
            color = color,
        )
    }

    fullSpanItem {
        TextBodyLarge(
            text = annotatedString("BodyLarge", isAnnotated),
            modifier = Modifier.padding(defaultItemPadding()),
            color = color,
        )
    }
    fullSpanItem {
        TextBodyMedium(
            text = annotatedString("BodyMedium", isAnnotated),
            modifier = Modifier.padding(defaultItemPadding()),
            color = color,
        )
    }
    fullSpanItem {
        TextBodySmall(
            text = annotatedString("BodySmall", isAnnotated),
            modifier = Modifier.padding(defaultItemPadding()),
            color = color,
        )
    }

    fullSpanItem {
        TextLabelLarge(
            text = annotatedString("LabelLarge", isAnnotated),
            modifier = Modifier.padding(defaultItemPadding()),
            color = color,
        )
    }
    fullSpanItem {
        TextLabelMedium(
            text = annotatedString("LabelMedium", isAnnotated),
            modifier = Modifier.padding(defaultItemPadding()),
            color = color,
        )
    }
    fullSpanItem {
        TextLabelSmall(
            text = annotatedString("LabelSmall", isAnnotated),
            modifier = Modifier.padding(defaultItemPadding()),
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
