package app.k9mail.feature.launcher

import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import app.k9mail.feature.account.edit.navigation.AccountEditRoute
import app.k9mail.feature.account.setup.navigation.AccountSetupRoute
import app.k9mail.feature.onboarding.main.navigation.OnboardingRoute
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.settings.api.AccountSettingsRoute
import net.thunderbird.feature.debug.settings.navigation.SecretDebugSettingsRoute
import net.thunderbird.feature.funding.api.FundingRoute
import net.thunderbird.feature.navigation.changelog.api.ChangeLogMode
import net.thunderbird.feature.navigation.changelog.api.ChangelogRoute

sealed class FeatureLauncherTarget(
    val deepLinkUri: Uri,
    val flags: Int? = null,
) {
    data class AccountEditIncomingSettings(val accountId: AccountId) : FeatureLauncherTarget(
        deepLinkUri = AccountEditRoute.IncomingServerSettings(accountId.toString()).route().toUri(),
    )

    data class AccountEditOutgoingSettings(val accountId: AccountId) : FeatureLauncherTarget(
        deepLinkUri = AccountEditRoute.OutgoingServerSettings(accountId.toString()).route().toUri(),
    )

    data object AccountSetup : FeatureLauncherTarget(
        deepLinkUri = AccountSetupRoute.AccountSetup().route().toUri(),
    )

    data class AccountSettings(val accountId: AccountId) : FeatureLauncherTarget(
        deepLinkUri = AccountSettingsRoute.GeneralSettings(accountId.toString()).route().toUri(),
    )

    data class AccountReadingMailSettings(val accountId: AccountId) : FeatureLauncherTarget(
        deepLinkUri = AccountSettingsRoute.ReadingMailSettings(accountId.toString()).route().toUri(),
    )

    data class AccountFetchingMailSettings(val accountId: AccountId) : FeatureLauncherTarget(
        deepLinkUri = AccountSettingsRoute.FetchingMailSettings(accountId.toString()).route().toUri(),
    )

    data class AccountAdvancedFetchingMailSettings(val accountId: AccountId) : FeatureLauncherTarget(
        deepLinkUri = AccountSettingsRoute.AdvancedFetchingMailSettings(accountId.toString()).route().toUri(),
    )

    data class AccountSearchSettings(val accountId: AccountId) : FeatureLauncherTarget(
        deepLinkUri = AccountSettingsRoute.SearchSettings(accountId.toString()).route().toUri(),
    )

    data object Funding : FeatureLauncherTarget(
        deepLinkUri = FundingRoute.Contribution.route().toUri(),
    )

    data class Changelog(val changeLogMode: ChangeLogMode) : FeatureLauncherTarget(
        deepLinkUri = ChangelogRoute(changeLogMode = changeLogMode).route().toUri(),
    )

    data object Onboarding : FeatureLauncherTarget(
        deepLinkUri = OnboardingRoute.Onboarding().route().toUri(),
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK,
    )

    data object SecretDebugSettingsNotification : FeatureLauncherTarget(
        deepLinkUri = SecretDebugSettingsRoute(tab = SecretDebugSettingsRoute.Tab.Notification).route().toUri(),
    )

    data object SecretDebugSettingsFeatureFlag : FeatureLauncherTarget(
        deepLinkUri = SecretDebugSettingsRoute(tab = SecretDebugSettingsRoute.Tab.FeatureFlag).route().toUri(),
    )
}
