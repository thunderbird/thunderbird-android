package app.k9mail.feature.launcher

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import app.k9mail.core.ui.compose.common.activity.setActivityContent
import app.k9mail.feature.account.setup.navigation.DEEPLINK_ACCOUNT_SETUP
import app.k9mail.feature.launcher.ui.FeatureLauncherApp
import app.k9mail.feature.onboarding.navigation.DEEPLINK_ONBOARDING

class FeatureLauncherActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setActivityContent {
            FeatureLauncherApp()
        }
    }

    companion object {
        @JvmStatic
        fun launchOnboarding(context: Activity) {
            val intent = Intent(context, FeatureLauncherActivity::class.java).apply {
                data = DEEPLINK_ONBOARDING.toUri()
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(intent)
        }

        @JvmStatic
        fun launchSetupAccount(context: Activity) {
            val intent = Intent(context, FeatureLauncherActivity::class.java).apply {
                data = DEEPLINK_ACCOUNT_SETUP.toUri()
            }
            context.startActivity(intent)
        }
    }
}
