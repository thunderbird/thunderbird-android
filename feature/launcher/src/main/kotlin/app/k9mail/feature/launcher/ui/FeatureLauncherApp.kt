package app.k9mail.feature.launcher.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import app.k9mail.core.ui.compose.common.activity.LocalActivity
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.theme.K9Theme
import app.k9mail.feature.launcher.FeatureLauncherExternalContract.FeatureThemeProvider
import app.k9mail.feature.launcher.navigation.FeatureLauncherNavHost
import org.koin.compose.koinInject

@Composable
fun FeatureLauncherApp(
    modifier: Modifier = Modifier,
    themeProvider: FeatureThemeProvider = koinInject<FeatureThemeProvider>(),
) {
    val navController = rememberNavController()

    K9Theme {
        themeProvider.WithTheme {
            Surface(
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
}
