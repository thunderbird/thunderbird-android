package app.k9mail.feature.preview.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
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
        Background(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .then(modifier),
        ) {
            FeatureNavHost(navController = navController)
        }
    }
}
