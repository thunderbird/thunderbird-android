package net.thunderbird.feature.mail.message.list.internal.ui.state.sideeffect.inject

import net.thunderbird.core.common.inject.factoryListOf
import net.thunderbird.feature.mail.message.list.internal.ui.state.sideeffect.AllConfigurationsReadySideEffect
import net.thunderbird.feature.mail.message.list.internal.ui.state.sideeffect.ChangeSortCriteriaSideEffect
import net.thunderbird.feature.mail.message.list.internal.ui.state.sideeffect.LoadFolderInformationSideEffect
import net.thunderbird.feature.mail.message.list.internal.ui.state.sideeffect.LoadPreferencesSideEffect
import net.thunderbird.feature.mail.message.list.internal.ui.state.sideeffect.LoadSortCriteriaStateSideEffectHandler
import net.thunderbird.feature.mail.message.list.internal.ui.state.sideeffect.LoadSwipeActionsStateSideEffectHandler
import net.thunderbird.feature.mail.message.list.internal.ui.state.sideeffect.ui.OpenMessageSideEffect
import net.thunderbird.feature.mail.message.list.internal.ui.state.sideeffect.ToggleMessageSideEffect
import net.thunderbird.feature.mail.message.list.internal.ui.state.sideeffect.legacy.LoadMessagesLegacySideEffect
import net.thunderbird.feature.mail.message.list.ui.MessageListContract
import net.thunderbird.feature.mail.message.list.ui.state.sideeffect.MessageListStateSideEffectHandlerFactory
import org.koin.dsl.module

/**
 * Dependency injection module that provides a list of side effect handler factories for the
 * message list feature.
 */
internal val messageListSideEffectsModule = module {
    factoryListOf<MessageListStateSideEffectHandlerFactory>(
        {
            LoadPreferencesSideEffect.Factory(
                logger = get(),
                getMessageListPreferences = get(),
            )
        },
        {
            LoadSwipeActionsStateSideEffectHandler.Factory(
                logger = get(),
                buildSwipeActions = get(),
            )
        },
        { parameters ->
            val args = parameters.get<MessageListContract.ViewModel.Args>()
            LoadSortCriteriaStateSideEffectHandler.Factory(
                accounts = args.accountIds,
                logger = get(),
                getSortCriteriaPerAccount = get(),
            )
        },
        {
            ChangeSortCriteriaSideEffect.Factory(
                logger = get(),
                updateSortCriteria = get(),
            )
        },
        { parameters ->
            val args = parameters.get<MessageListContract.ViewModel.Args>()
            LoadFolderInformationSideEffect.Factory(
                accountIds = args.accountIds,
                folderId = args.folderId,
                logger = get(),
                folderRepository = get(),
            )
        },
        { AllConfigurationsReadySideEffect.Factory(logger = get()) },
        { parameters ->
            val args = parameters.get<MessageListContract.ViewModel.Args>()
            LoadMessagesLegacySideEffect.Factory(logger = get(), legacyBridge = args.legacyMessageListBridge)
        },
        { OpenMessageSideEffect.Factory(logger = get()) },
        { ToggleMessageSideEffect.Factory(logger = get()) },
    )
}
