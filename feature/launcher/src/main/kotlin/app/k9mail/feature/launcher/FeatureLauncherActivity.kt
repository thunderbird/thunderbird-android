package app.k9mail.feature.launcher

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import app.k9mail.core.ui.compose.common.activity.setActivityContent
import app.k9mail.core.ui.compose.common.navigation.toDeepLinkUri
import app.k9mail.feature.account.edit.navigation.NAVIGATION_ROUTE_ACCOUNT_EDIT_CONFIG_INCOMING
import app.k9mail.feature.account.edit.navigation.NAVIGATION_ROUTE_ACCOUNT_EDIT_CONFIG_OUTGOING
import app.k9mail.feature.account.edit.navigation.withAccountUuid
import app.k9mail.feature.account.setup.navigation.NAVIGATION_ROUTE_ACCOUNT_SETUP
import app.k9mail.feature.launcher.ui.FeatureLauncherApp
import app.k9mail.feature.onboarding.navigation.NAVIGATION_ROUTE_ONBOARDING

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
        fun launchOnboarding(context: Context) {
            val intent = Intent(context, FeatureLauncherActivity::class.java).apply {
                data = NAVIGATION_ROUTE_ONBOARDING.toDeepLinkUri()
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(intent)
        }

        @JvmStatic
        fun launchSetupAccount(context: Context) {
            val intent = Intent(context, FeatureLauncherActivity::class.java).apply {
                data = NAVIGATION_ROUTE_ACCOUNT_SETUP.toDeepLinkUri()
            }
            context.startActivity(intent)
        }

        @JvmStatic
        fun launchEditIncomingSettings(context: Context, accountUuid: String) {
            val intent = Intent(context, FeatureLauncherActivity::class.java).apply {
                data = NAVIGATION_ROUTE_ACCOUNT_EDIT_CONFIG_INCOMING.withAccountUuid(accountUuid).toDeepLinkUri()
            }
            context.startActivity(intent)
        }

        @JvmStatic
        fun launchEditOutgoingSettings(context: Context, accountUuid: String) {
            val intent = Intent(context, FeatureLauncherActivity::class.java).apply {
                data = NAVIGATION_ROUTE_ACCOUNT_EDIT_CONFIG_OUTGOING.withAccountUuid(accountUuid).toDeepLinkUri()
            }
            context.startActivity(intent)
        }
    }
}
