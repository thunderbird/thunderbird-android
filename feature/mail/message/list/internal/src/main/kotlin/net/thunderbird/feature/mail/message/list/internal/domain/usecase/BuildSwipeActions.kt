package net.thunderbird.feature.mail.message.list.internal.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.common.action.SwipeAction
import net.thunderbird.core.common.action.SwipeActions
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.message.list.domain.DomainContract

internal class BuildSwipeActions(
    generalSettingsManager: GeneralSettingsManager,
    private val accountManager: LegacyAccountManager,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + mainDispatcher),
) : DomainContract.UseCase.BuildSwipeActions {
    @OptIn(ExperimentalCoroutinesApi::class)
    val swipeActions: StateFlow<Map<AccountId, SwipeActions>> = generalSettingsManager.getConfigFlow()
        .flatMapConcat { config ->
            val defaultSwipeActions = config.interaction.swipeActions
            val shouldShowSetupArchiveFolderDialog = config.display.miscSettings.shouldShowSetupArchiveFolderDialog

            accountManager
                .getAll()
                .map { accounts ->
                    accounts
                        .associate { account ->
                            account.id to SwipeActions(
                                leftAction = buildSwipeAction(
                                    account = account,
                                    defaultSwipeAction = defaultSwipeActions.leftAction,
                                    shouldShowSetupArchiveFolderDialog = shouldShowSetupArchiveFolderDialog,
                                ),
                                rightAction = buildSwipeAction(
                                    account = account,
                                    defaultSwipeAction = defaultSwipeActions.rightAction,
                                    shouldShowSetupArchiveFolderDialog = shouldShowSetupArchiveFolderDialog,
                                ),
                            )
                        }
                }
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = emptyMap(),
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun invoke(): StateFlow<Map<AccountId, SwipeActions>> = swipeActions

    private fun buildSwipeAction(
        account: LegacyAccount,
        defaultSwipeAction: SwipeAction,
        shouldShowSetupArchiveFolderDialog: Boolean,
    ): SwipeAction = when (defaultSwipeAction) {
        SwipeAction.Archive if account.isIncomingServerPop3() -> SwipeAction.ArchiveDisabled

        SwipeAction.Archive if account.hasArchiveFolder().not() && shouldShowSetupArchiveFolderDialog ->
            SwipeAction.ArchiveSetupArchiveFolder

        SwipeAction.Archive if account.hasArchiveFolder().not() -> SwipeAction.ArchiveDisabled

        else -> defaultSwipeAction
    }
}
