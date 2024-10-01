package app.k9mail.feature.launcher

import android.content.Intent
import android.net.Uri
import app.k9mail.core.ui.compose.common.navigation.toDeepLinkUri
import app.k9mail.feature.account.edit.navigation.withAccountUuid
import app.k9mail.feature.account.setup.navigation.NAVIGATION_ROUTE_ACCOUNT_SETUP
import app.k9mail.feature.funding.api.FundingRoute
import app.k9mail.feature.onboarding.main.navigation.NAVIGATION_ROUTE_ONBOARDING

sealed class FeatureLauncherTarget(
    val deepLinkUri: Uri,
    val flags: Int? = null,
) {
    data object Onboarding : FeatureLauncherTarget(
        deepLinkUri = NAVIGATION_ROUTE_ONBOARDING.toDeepLinkUri(),
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK,
    )

    data object AccountSetup : FeatureLauncherTarget(
        deepLinkUri = NAVIGATION_ROUTE_ACCOUNT_SETUP.toDeepLinkUri(),
    )

    data class AccountEditIncomingSettings(val accountUuid: String) : FeatureLauncherTarget(
        deepLinkUri = NAVIGATION_ROUTE_ACCOUNT_SETUP.withAccountUuid(accountUuid).toDeepLinkUri(),
    )

    data class AccountEditOutgoingSettings(val accountUuid: String) : FeatureLauncherTarget(
        deepLinkUri = NAVIGATION_ROUTE_ACCOUNT_SETUP.withAccountUuid(accountUuid).toDeepLinkUri(),
    )

    data object Funding : FeatureLauncherTarget(
        deepLinkUri = FundingRoute.Overview.toDeepLinkUri(),
    )
}
