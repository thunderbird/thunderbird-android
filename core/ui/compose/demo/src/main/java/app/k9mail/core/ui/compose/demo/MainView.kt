package app.k9mail.core.ui.compose.demo

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.theme.K9Theme
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.core.ui.compose.theme.ThunderbirdTheme

@Composable
fun MainView(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
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
fun MainContent(
    name: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier),
        color = MainTheme.colors.background,
    ) {
        Column {
            Text(text = "Hello $name!")
            Image(painter = painterResource(id = MainTheme.images.logo), contentDescription = "logo")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun Preview() {
    MainView()
}
