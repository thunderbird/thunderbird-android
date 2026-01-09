package net.thunderbird.feature.mail.message.list.internal

import net.thunderbird.core.common.inject.factoryListOf
import net.thunderbird.core.common.inject.getList
import net.thunderbird.feature.mail.message.list.domain.DomainContract
import net.thunderbird.feature.mail.message.list.internal.domain.usecase.BuildSwipeActions
import net.thunderbird.feature.mail.message.list.internal.domain.usecase.CreateArchiveFolder
import net.thunderbird.feature.mail.message.list.internal.domain.usecase.GetAccountFolders
import net.thunderbird.feature.mail.message.list.internal.domain.usecase.SetArchiveFolder
import net.thunderbird.feature.mail.message.list.internal.ui.MessageListViewModel
import net.thunderbird.feature.mail.message.list.internal.ui.dialog.SetupArchiveFolderDialogFragment
import net.thunderbird.feature.mail.message.list.internal.ui.dialog.SetupArchiveFolderDialogViewModel
import net.thunderbird.feature.mail.message.list.internal.ui.state.machine.MessageListStateMachine
import net.thunderbird.feature.mail.message.list.internal.ui.state.sideeffect.LoadSwipeActionsStateSideEffectHandler
import net.thunderbird.feature.mail.message.list.ui.MessageListContract
import net.thunderbird.feature.mail.message.list.ui.MessageListStateSideEffectHandlerFactory
import net.thunderbird.feature.mail.message.list.ui.dialog.SetupArchiveFolderDialogContract
import net.thunderbird.feature.mail.message.list.ui.dialog.SetupArchiveFolderDialogFragmentFactory
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val featureMessageListModule = module {
    factory<DomainContract.UseCase.GetAccountFolders> { GetAccountFolders(folderRepository = get()) }
    factory<DomainContract.UseCase.CreateArchiveFolder> {
        CreateArchiveFolder(
            accountManager = get(),
            backendStorageFactory = get(),
            specialFolderUpdaterFactory = get(),
            remoteFolderCreatorFactory = get(),
        )
    }
    factory<DomainContract.UseCase.SetArchiveFolder> {
        SetArchiveFolder(
            accountManager = get(),
            backendStorageFactory = get(),
            specialFolderUpdaterFactory = get(),
        )
    }
    factory<DomainContract.UseCase.BuildSwipeActions> {
        BuildSwipeActions(
            generalSettingsManager = get(),
            accountManager = get(),
        )
    }
    viewModel { parameters ->
        SetupArchiveFolderDialogViewModel(
            accountUuid = parameters.get(),
            logger = get(),
            getAccountFolders = get(),
            createArchiveFolder = get(),
            setArchiveFolder = get(),
            resourceManager = get(),
            generalSettingsManager = get(),
        ) as SetupArchiveFolderDialogContract.ViewModel
    }
    factory<SetupArchiveFolderDialogFragmentFactory> { SetupArchiveFolderDialogFragment.Factory }
    factoryListOf<MessageListStateSideEffectHandlerFactory>(
        {
            LoadSwipeActionsStateSideEffectHandler.Factory(
                logger = get(),
                buildSwipeActions = get(),
            )
        },
    )
    factory { MessageListStateMachine.Factory() }
    viewModel<MessageListContract.ViewModel> {
        MessageListViewModel(
            logger = get(),
            messageListStateMachineFactory = get(),
            stateSideEffectHandlersFactories = getList(),
        )
    }
}
