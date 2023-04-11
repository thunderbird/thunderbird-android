package app.k9mail.feature.preview.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import app.k9mail.core.ui.compose.designsystem.atom.Background
import app.k9mail.core.ui.compose.theme.K9Theme
import app.k9mail.feature.preview.navigation.FeatureNavHost

@Composable
fun FeatureApp(
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()

    K9Theme {
        val contentPadding = WindowInsets.systemBars.asPaddingValues()

        Background(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .then(modifier),
        ) {
            FeatureNavHost(navController = navController)
        }
    }
}
