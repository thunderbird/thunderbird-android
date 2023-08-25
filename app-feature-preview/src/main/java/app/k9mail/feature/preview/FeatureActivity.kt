package app.k9mail.feature.preview

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import app.k9mail.core.ui.compose.common.activity.setActivityContent
import app.k9mail.feature.preview.ui.FeatureApp

class FeatureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setActivityContent {
            FeatureApp()
        }
    }
}
