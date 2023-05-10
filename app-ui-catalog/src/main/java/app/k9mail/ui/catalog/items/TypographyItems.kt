package app.k9mail.ui.catalog.items

import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBody1
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

fun LazyGridScope.typographyItems() {
    sectionHeaderItem(text = "Typography")
    sectionSubtitleItem(text = "Normal")
    item { TextHeadline1(text = "Headline1") }
    item { TextHeadline2(text = "Headline2") }
    item { TextHeadline3(text = "Headline3") }
    item { TextHeadline4(text = "Headline4") }
    item { TextHeadline5(text = "Headline5") }
    item { TextHeadline6(text = "Headline6") }
    item { TextSubtitle1(text = "Subtitle1") }
    item { TextSubtitle2(text = "Subtitle2") }
    item { TextBody1(text = "Body1") }
    item { TextBody2(text = "Body2") }
    item { TextButton(text = "Button") }
    item { TextCaption(text = "Caption") }
    item { TextOverline(text = "Overline") }
    sectionSubtitleItem(text = "colored")
    item { TextHeadline1(text = "Headline1", color = Color.Magenta) }
    item { TextHeadline2(text = "Headline2", color = Color.Magenta) }
    item { TextHeadline3(text = "Headline3", color = Color.Magenta) }
    item { TextHeadline4(text = "Headline4", color = Color.Magenta) }
    item { TextHeadline5(text = "Headline5", color = Color.Magenta) }
    item { TextHeadline6(text = "Headline6", color = Color.Magenta) }
    item { TextSubtitle1(text = "Subtitle1", color = Color.Magenta) }
    item { TextSubtitle2(text = "Subtitle2", color = Color.Magenta) }
    item { TextBody1(text = "Body1", color = Color.Magenta) }
    item { TextBody2(text = "Body2", color = Color.Magenta) }
    item { TextButton(text = "Button", color = Color.Magenta) }
    item { TextCaption(text = "Caption", color = Color.Magenta) }
    item { TextOverline(text = "Overline", color = Color.Magenta) }
    sectionSubtitleItem(text = "Annotated")
    item { TextHeadline1(text = annotatedString("Headline1")) }
    item { TextHeadline2(text = annotatedString("Headline2")) }
    item { TextHeadline3(text = annotatedString("Headline3")) }
    item { TextHeadline4(text = annotatedString("Headline4")) }
    item { TextHeadline5(text = annotatedString("Headline5")) }
    item { TextHeadline6(text = annotatedString("Headline6")) }
    item { TextSubtitle1(text = annotatedString("Subtitle1")) }
    item { TextSubtitle2(text = annotatedString("Subtitle2")) }
    item { TextBody1(text = annotatedString("Body1")) }
    item { TextBody2(text = annotatedString("Body2")) }
    item { TextButton(text = annotatedString("Button")) }
    item { TextCaption(text = annotatedString("Caption")) }
    item { TextOverline(text = annotatedString("Overline")) }
}

private fun annotatedString(name: String) = buildAnnotatedString {
    append(name)
    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
        append("Annotated")
    }
}
