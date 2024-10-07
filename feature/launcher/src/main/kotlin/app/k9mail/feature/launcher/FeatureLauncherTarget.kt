package app.k9mail.feature.launcher

import android.content.Intent
import android.net.Uri
import app.k9mail.core.ui.compose.common.navigation.toDeepLinkUri
import app.k9mail.feature.account.edit.navigation.withAccountUuid
import app.k9mail.feature.account.setup.navigation.NAVIGATION_ROUTE_ACCOUNT_SETUP
import app.k9mail.feature.onboarding.main.navigation.NAVIGATION_ROUTE_ONBOARDING

sealed class FeatureLauncherTarget(
    val route: String,
    val flags: Int? = null,
) {
    data object Onboarding : FeatureLauncherTarget(
        route = NAVIGATION_ROUTE_ONBOARDING,
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK,
    )

    data object AccountSetup : FeatureLauncherTarget(
        route = NAVIGATION_ROUTE_ACCOUNT_SETUP,
    )

    data class AccountEditIncomingSettings(val accountUuid: String) : FeatureLauncherTarget(
        route = NAVIGATION_ROUTE_ACCOUNT_SETUP,
    )

    data class AccountEditOutgoingSettings(val accountUuid: String) : FeatureLauncherTarget(
        route = NAVIGATION_ROUTE_ACCOUNT_SETUP,
    )

    data object Funding : FeatureLauncherTarget(
        route = "TODO",
    )

    fun toDeepLinkUri(): Uri {
        return when (this) {
            is AccountEditIncomingSettings -> route.withAccountUuid(accountUuid).toDeepLinkUri()
            is AccountEditOutgoingSettings -> route.withAccountUuid(accountUuid).toDeepLinkUri()

            else -> route.toDeepLinkUri()
        }
    }
}
