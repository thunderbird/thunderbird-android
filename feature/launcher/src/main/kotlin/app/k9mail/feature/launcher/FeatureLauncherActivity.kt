package app.k9mail.feature.launcher

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import app.k9mail.core.ui.compose.common.activity.setActivityContent
import app.k9mail.core.ui.compose.common.navigation.toDeepLinkUri
import app.k9mail.feature.account.edit.navigation.NAVIGATION_ROUTE_ACCOUNT_EDIT_SERVER_SETTINGS_INCOMING
import app.k9mail.feature.account.edit.navigation.NAVIGATION_ROUTE_ACCOUNT_EDIT_SERVER_SETTINGS_OUTGOING
import app.k9mail.feature.account.edit.navigation.withAccountUuid
import app.k9mail.feature.account.setup.navigation.NAVIGATION_ROUTE_ACCOUNT_SETUP
import app.k9mail.feature.launcher.ui.FeatureLauncherApp
import app.k9mail.feature.onboarding.main.navigation.NAVIGATION_ROUTE_ONBOARDING
import net.discdd.k9.onboarding.navigation.NAVIGATION_ROUTE_DDD_ONBOARDING
import com.fsck.k9.ui.base.K9Activity

class FeatureLauncherActivity : K9Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setActivityContent {
            FeatureLauncherApp()
        }
    }

    companion object {
        @JvmStatic fun launchDddOnboarding(context: Context) {
            val intent = Intent(context, FeatureLauncherActivity::class.java).apply {
                data = NAVIGATION_ROUTE_DDD_ONBOARDING.toDeepLinkUri()
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(intent)
        }

        @JvmStatic fun launchOnboarding(context: Context) {
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
        fun getEditIncomingSettingsIntent(context: Context, accountUuid: String): Intent {
            val intent = Intent(context, FeatureLauncherActivity::class.java).apply {
                data = NAVIGATION_ROUTE_ACCOUNT_EDIT_SERVER_SETTINGS_INCOMING
                    .withAccountUuid(accountUuid).toDeepLinkUri()
            }
            return intent
        }

        @JvmStatic
        fun launchEditIncomingSettings(context: Context, accountUuid: String) {
            val intent = getEditIncomingSettingsIntent(context, accountUuid)
            context.startActivity(intent)
        }

        @JvmStatic
        fun getEditOutgoingSettingsIntent(context: Context, accountUuid: String): Intent {
            val intent = Intent(context, FeatureLauncherActivity::class.java).apply {
                data = NAVIGATION_ROUTE_ACCOUNT_EDIT_SERVER_SETTINGS_OUTGOING
                    .withAccountUuid(accountUuid).toDeepLinkUri()
            }
            return intent
        }

        @JvmStatic
        fun launchEditOutgoingSettings(context: Context, accountUuid: String) {
            val intent = getEditOutgoingSettingsIntent(context, accountUuid)
            context.startActivity(intent)
        }
    }
}
