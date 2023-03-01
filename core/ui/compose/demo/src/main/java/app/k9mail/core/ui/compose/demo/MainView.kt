package app.k9mail.core.ui.compose.demo

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.button.Button
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonOutlined
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonText
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
import app.k9mail.core.ui.compose.theme.K9Theme
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.core.ui.compose.theme.ThunderbirdTheme

@Composable
fun MainView(
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .then(modifier),
    ) {
        K9Theme {
            MainContent(name = "K-9")
        }
        K9Theme(darkTheme = true) {
            MainContent(name = "K-9 dark")
        }
        ThunderbirdTheme {
            MainContent(name = "Thunderbird")
        }
        ThunderbirdTheme(darkTheme = true) {
            MainContent(name = "Thunderbird dark")
        }
    }
}

@Composable
private fun MainContent(
    name: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier),
    ) {
        Column {
            TextHeadline4(text = "$name theme")
            Image(painter = painterResource(id = MainTheme.images.logo), contentDescription = "logo")

            ButtonContent()
            TypographyContent()
        }
    }
}

@Composable
private fun ButtonContent() {
    Column {
        TextHeadline6(text = "Buttons")

        Button(text = "Button", onClick = { })
        ButtonOutlined(text = "ButtonOutlined", onClick = { })
        ButtonText(text = "ButtonText", onClick = { })
    }
}

@Composable
private fun TypographyContent() {
    Column {
        TextHeadline6(text = "Typography")

        TextHeadline1(text = "Headline1")
        TextHeadline2(text = "Headline2")
        TextHeadline3(text = "Headline3")
        TextHeadline4(text = "Headline4")
        TextHeadline5(text = "Headline5")
        TextHeadline6(text = "Headline6")
        TextSubtitle1(text = "Subtitle1")
        TextSubtitle2(text = "Subtitle2")
        TextBody1(text = "Body1")
        TextBody2(text = "Body2")
        TextButton(text = "Button")
        TextCaption(text = "Caption")
        TextOverline(text = "Overline")
    }
}

@Preview(showBackground = true)
@Composable
fun Preview() {
    MainView()
}
