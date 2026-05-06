package com.fsck.k9.ui.settings

import androidx.navigation.NavGraphBuilder
import androidx.navigation.toRoute
import com.fsck.k9.ui.changelog.ChangelogContract
import com.fsck.k9.ui.changelog.ChangelogScreen
import com.fsck.k9.ui.changelog.ChangelogViewModel
import net.thunderbird.core.ui.contract.mvi.observe
import net.thunderbird.core.ui.navigation.deepLinkComposable
import net.thunderbird.feature.navigation.changelog.api.ChangelogNavigation
import net.thunderbird.feature.navigation.changelog.api.ChangelogRoute
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

internal class DefaultChangelogNavigation : ChangelogNavigation {

    override fun registerRoutes(
        navGraphBuilder: NavGraphBuilder,
        onBack: () -> Unit,
        onFinish: (ChangelogRoute) -> Unit,
    ) {
        with(navGraphBuilder) {
            deepLinkComposable<ChangelogRoute>(
                basePath = ChangelogRoute.BASE_PATH,
            ) { backStackEntry ->
                val changelogRoute = backStackEntry.toRoute<ChangelogRoute>()

                val viewModel: ChangelogViewModel = koinViewModel(
                    parameters = { parametersOf(changelogRoute.changeLogMode) },
                )
                val (state, dispatch) = viewModel.observe {}
                ChangelogScreen(
                    releaseItems = state.value.releaseItems,
                    showRecentChanges = state.value.showRecentChanges,
                    onShowRecentChangesCheck = {
                        dispatch(ChangelogContract.Event.OnShowRecentChangesCheck(state.value.showRecentChanges))
                    },
                    onBack = onBack,
                )
            }
        }
    }
}
