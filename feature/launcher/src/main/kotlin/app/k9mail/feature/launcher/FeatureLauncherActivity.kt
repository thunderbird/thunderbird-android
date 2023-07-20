package app.k9mail.feature.launcher

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import app.k9mail.core.ui.compose.common.activity.setActivityContent
import app.k9mail.feature.account.setup.navigation.NAVIGATION_ROUTE_ACCOUNT_SETUP
import app.k9mail.feature.launcher.ui.FeatureLauncherApp
import app.k9mail.feature.onboarding.navigation.NAVIGATION_ROUTE_ONBOARDING

class FeatureLauncherActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val destination = intent.getStringExtra(EXTRA_DESTINATION)

        setActivityContent {
            FeatureLauncherApp(startDestination = destination)
        }
    }

    companion object {
        private const val EXTRA_DESTINATION = "destination"
        private const val DESTINATION_ONBOARDING = NAVIGATION_ROUTE_ONBOARDING
        private const val DESTINATION_SETUP_ACCOUNT = NAVIGATION_ROUTE_ACCOUNT_SETUP

        @JvmStatic
        fun launchOnboarding(context: Activity) {
            val intent = Intent(context, FeatureLauncherActivity::class.java).apply {
                putExtra(EXTRA_DESTINATION, DESTINATION_ONBOARDING)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(intent)
        }

        @JvmStatic
        fun launchSetupAccount(context: Activity) {
            val intent = Intent(context, FeatureLauncherActivity::class.java).apply {
                putExtra(EXTRA_DESTINATION, DESTINATION_SETUP_ACCOUNT)
            }
            context.startActivity(intent)
        }
    }
}
