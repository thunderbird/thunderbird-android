package app.k9mail.feature.launcher.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.feature.launcher.navigation.FeatureLauncherNavHost
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider
import org.koin.compose.koinInject

@Composable
fun FeatureLauncherApp(
    modifier: Modifier = Modifier,
    themeProvider: FeatureThemeProvider = koinInject<FeatureThemeProvider>(),
) {
    val navController = rememberNavController()

    themeProvider.WithTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .then(modifier),
        ) {
            val activity = LocalActivity.current as ComponentActivity

            FeatureLauncherNavHost(
                navController = navController,
                onBack = { activity.finish() },
            )
        }
    }
}
