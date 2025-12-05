package net.thunderbird.feature.mail.message.list.internal.domain.usecase

import net.thunderbird.core.common.action.SwipeAction
import net.thunderbird.core.common.action.SwipeActions
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.feature.mail.account.api.AccountManager
import net.thunderbird.feature.mail.account.api.BaseAccount
import net.thunderbird.feature.mail.message.list.domain.DomainContract

internal class BuildSwipeActions(
    private val generalSettingsManager: GeneralSettingsManager,
    private val accountManager: AccountManager<BaseAccount>,
) : DomainContract.UseCase.BuildSwipeActions<BaseAccount> {
    private val defaultSwipeActions get() = generalSettingsManager.getConfig().interaction.swipeActions

    override fun invoke(
        accountUuids: Set<String>,
        isIncomingServerPop3: (BaseAccount) -> Boolean,
        hasArchiveFolder: (BaseAccount) -> Boolean,
    ): Map<String, SwipeActions> {
        val shouldShowSetupArchiveFolderDialog = generalSettingsManager
            .getConfig().display.miscSettings
            .shouldShowSetupArchiveFolderDialog
        return accountUuids
            .mapNotNull { uuid -> accountManager.getAccount(uuid) }
            .associate { account ->
                account.uuid to SwipeActions(
                    leftAction = buildSwipeAction(
                        account = account,
                        defaultSwipeAction = defaultSwipeActions.leftAction,
                        isIncomingServerPop3 = isIncomingServerPop3,
                        hasArchiveFolder = hasArchiveFolder,
                        shouldShowSetupArchiveFolderDialog = shouldShowSetupArchiveFolderDialog,
                    ),
                    rightAction = buildSwipeAction(
                        account = account,
                        defaultSwipeAction = defaultSwipeActions.rightAction,
                        isIncomingServerPop3 = isIncomingServerPop3,
                        hasArchiveFolder = hasArchiveFolder,
                        shouldShowSetupArchiveFolderDialog = shouldShowSetupArchiveFolderDialog,
                    ),
                )
            }
    }

    private fun buildSwipeAction(
        account: BaseAccount,
        defaultSwipeAction: SwipeAction,
        isIncomingServerPop3: BaseAccount.() -> Boolean,
        hasArchiveFolder: BaseAccount.() -> Boolean,
        shouldShowSetupArchiveFolderDialog: Boolean,
    ): SwipeAction = when (defaultSwipeAction) {
        SwipeAction.Archive if account.isIncomingServerPop3() -> SwipeAction.ArchiveDisabled

        SwipeAction.Archive if account.hasArchiveFolder().not() && shouldShowSetupArchiveFolderDialog ->
            SwipeAction.ArchiveSetupArchiveFolder

        SwipeAction.Archive if account.hasArchiveFolder().not() -> SwipeAction.ArchiveDisabled

        else -> defaultSwipeAction
    }
}
