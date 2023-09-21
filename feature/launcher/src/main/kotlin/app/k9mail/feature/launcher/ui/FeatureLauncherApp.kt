package app.k9mail.feature.launcher.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import app.k9mail.core.ui.compose.common.activity.LocalActivity
import app.k9mail.core.ui.compose.designsystem.atom.Background
import app.k9mail.core.ui.compose.theme.K9Theme
import app.k9mail.feature.launcher.navigation.FeatureLauncherNavHost

@Composable
fun FeatureLauncherApp(
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
            val activity = LocalActivity.current

            FeatureLauncherNavHost(
                navController = navController,
                onBack = { activity.finish() },
            )
        }
    }
}
