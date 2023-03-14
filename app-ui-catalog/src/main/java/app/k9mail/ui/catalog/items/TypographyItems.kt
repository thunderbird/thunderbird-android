package app.k9mail.ui.catalog.items

import androidx.compose.foundation.lazy.grid.LazyGridScope
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
}
