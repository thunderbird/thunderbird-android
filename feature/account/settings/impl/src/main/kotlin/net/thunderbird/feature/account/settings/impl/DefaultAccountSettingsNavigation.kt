package net.thunderbird.feature.account.settings.impl

import androidx.navigation.NavGraphBuilder
import androidx.navigation.toRoute
import net.thunderbird.core.ui.navigation.deepLinkComposable
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.settings.api.AccountSettingsNavigation
import net.thunderbird.feature.account.settings.api.AccountSettingsRoute
import net.thunderbird.feature.account.settings.impl.ui.general.GeneralSettingsScreen
import net.thunderbird.feature.account.settings.impl.ui.readingMail.ReadingMailSettingsScreen
import net.thunderbird.feature.account.settings.impl.ui.search.SearchSettingsScreen

internal class DefaultAccountSettingsNavigation : AccountSettingsNavigation {

    override fun registerRoutes(
        navGraphBuilder: NavGraphBuilder,
        onBack: () -> Unit,
        onFinish: (AccountSettingsRoute) -> Unit,
    ) {
        with(navGraphBuilder) {
            deepLinkComposable<AccountSettingsRoute.GeneralSettings>(
                basePath = AccountSettingsRoute.GeneralSettings.BASE_PATH,
            ) { backStackEntry ->
                val generalSettingsRoute = backStackEntry.toRoute<AccountSettingsRoute.GeneralSettings>()
                val accountId = AccountIdFactory.of(generalSettingsRoute.accountId)

                GeneralSettingsScreen(
                    accountId = accountId,
                    onBack = onBack,
                )
            }
        }

        with(navGraphBuilder) {
            deepLinkComposable<AccountSettingsRoute.ReadingMailSettings>(
                basePath = AccountSettingsRoute.ReadingMailSettings.BASE_PATH,
            ) { backStackEntry ->
                val readingMailSettingsRoute = backStackEntry.toRoute<AccountSettingsRoute.ReadingMailSettings>()
                val accountId = AccountIdFactory.of(readingMailSettingsRoute.accountId)

                ReadingMailSettingsScreen(
                    accountId = accountId,
                    onBack = onBack,
                )
            }
        }

        with(navGraphBuilder) {
            deepLinkComposable<AccountSettingsRoute.SearchSettings>(
                basePath = AccountSettingsRoute.SearchSettings.Companion.BASE_PATH,
            ) { backStackEntry ->
                val searchSettingsRoute = backStackEntry.toRoute<AccountSettingsRoute.SearchSettings>()
                val accountId = AccountIdFactory.of(searchSettingsRoute.accountId)

                SearchSettingsScreen(
                    accountId = accountId,
                    onBack = onBack,
                )
            }
        }
    }
}
