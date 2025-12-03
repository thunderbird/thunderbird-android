package net.thunderbird.feature.mail.message.list.internal

import net.thunderbird.core.common.inject.factoryListOf
import net.thunderbird.feature.mail.message.list.domain.DomainContract
import net.thunderbird.feature.mail.message.list.internal.domain.usecase.BuildSwipeActions
import net.thunderbird.feature.mail.message.list.internal.domain.usecase.CreateArchiveFolder
import net.thunderbird.feature.mail.message.list.internal.domain.usecase.GetAccountFolders
import net.thunderbird.feature.mail.message.list.internal.domain.usecase.GetMessageListPreferences
import net.thunderbird.feature.mail.message.list.internal.domain.usecase.GetSortTypes
import net.thunderbird.feature.mail.message.list.internal.domain.usecase.SetArchiveFolder
import net.thunderbird.feature.mail.message.list.internal.ui.dialog.SetupArchiveFolderDialogFragment
import net.thunderbird.feature.mail.message.list.internal.ui.dialog.SetupArchiveFolderDialogViewModel
import net.thunderbird.feature.mail.message.list.internal.ui.state.sideeffect.LoadSortTypeStateSideEffectHandler
import net.thunderbird.feature.mail.message.list.internal.ui.state.sideeffect.LoadSwipeActionsStateSideEffectHandler
import net.thunderbird.feature.mail.message.list.internal.ui.state.sideeffect.StateSideEffectHandler
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
    factory<DomainContract.UseCase.GetMessageListPreferences> {
        GetMessageListPreferences(
            displayPreferenceManager = get(),
            interactionPreferenceManager = get(),
        )
    }
    factory<DomainContract.UseCase.GetSortTypes> {
        GetSortTypes(
            accountManager = get(),
            getDefaultSortType = get(),
        )
    }
    factoryListOf<StateSideEffectHandler.Factory>(
        { parameters ->
            LoadSwipeActionsStateSideEffectHandler.Factory(
                logger = get(),
                buildSwipeActions = get(),
            )
        },
        { parameters ->
            LoadSortTypeStateSideEffectHandler.Factory(
                accounts = parameters.get(),
                logger = get(),
                getSortTypes = get(),
            )
        },
    )
}
