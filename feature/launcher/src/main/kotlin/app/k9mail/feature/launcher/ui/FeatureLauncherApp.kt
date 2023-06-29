package app.k9mail.feature.launcher.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import app.k9mail.core.ui.compose.designsystem.atom.Background
import app.k9mail.core.ui.compose.theme.K9Theme
import app.k9mail.feature.launcher.navigation.FeatureLauncherNavHost

@Composable
fun FeatureLauncherApp(
    startDestination: String?,
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
            FeatureLauncherNavHost(
                navController = navController,
                startDestination = startDestination,
            )
        }
    }
}
