package app.k9mail.feature.launcher

import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import app.k9mail.core.ui.compose.common.navigation.toDeepLinkUri
import app.k9mail.feature.account.edit.navigation.NAVIGATION_ROUTE_ACCOUNT_EDIT_SERVER_SETTINGS_INCOMING
import app.k9mail.feature.account.edit.navigation.NAVIGATION_ROUTE_ACCOUNT_EDIT_SERVER_SETTINGS_OUTGOING
import app.k9mail.feature.account.edit.navigation.withAccountUuid
import app.k9mail.feature.account.setup.navigation.AccountSetupRoute
import app.k9mail.feature.funding.api.FundingRoute
import app.k9mail.feature.onboarding.main.navigation.OnboardingRoute

sealed class FeatureLauncherTarget(
    val deepLinkUri: Uri,
    val flags: Int? = null,
) {
    data object Onboarding : FeatureLauncherTarget(
        deepLinkUri = OnboardingRoute.Onboarding().route().toUri(),
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK,
    )

    data object AccountSetup : FeatureLauncherTarget(
        deepLinkUri = AccountSetupRoute.AccountSetup().route().toUri(),
    )

    data class AccountEditIncomingSettings(val accountUuid: String) : FeatureLauncherTarget(
        deepLinkUri = NAVIGATION_ROUTE_ACCOUNT_EDIT_SERVER_SETTINGS_INCOMING
            .withAccountUuid(accountUuid).toDeepLinkUri(),
    )

    data class AccountEditOutgoingSettings(val accountUuid: String) : FeatureLauncherTarget(
        deepLinkUri = NAVIGATION_ROUTE_ACCOUNT_EDIT_SERVER_SETTINGS_OUTGOING
            .withAccountUuid(accountUuid).toDeepLinkUri(),
    )

    data object Funding : FeatureLauncherTarget(
        deepLinkUri = FundingRoute.Contribution.route().toUri(),
    )
}
