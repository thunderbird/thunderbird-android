package net.thunderbird.feature.mail.message.list

import net.thunderbird.backend.api.BackendStorageFactory
import net.thunderbird.feature.mail.account.api.AccountManager
import net.thunderbird.feature.mail.account.api.BaseAccount
import net.thunderbird.feature.mail.folder.api.SpecialFolderUpdater
import net.thunderbird.feature.mail.message.list.domain.DomainContract
import net.thunderbird.feature.mail.message.list.domain.usecase.BuildSwipeActions
import net.thunderbird.feature.mail.message.list.domain.usecase.CreateArchiveFolder
import net.thunderbird.feature.mail.message.list.domain.usecase.GetAccountFolders
import net.thunderbird.feature.mail.message.list.domain.usecase.SetArchiveFolder
import net.thunderbird.feature.mail.message.list.ui.dialog.SetupArchiveFolderDialogFragment
import net.thunderbird.feature.mail.message.list.ui.dialog.SetupArchiveFolderDialogFragmentFactory
import net.thunderbird.feature.mail.message.list.ui.dialog.SetupArchiveFolderDialogViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val featureMessageModule = module {
    factory<DomainContract.UseCase.GetAccountFolders> { GetAccountFolders(folderRepository = get()) }
    factory<DomainContract.UseCase.CreateArchiveFolder> {
        CreateArchiveFolder(
            baseAccountManager = get<AccountManager<BaseAccount>>(),
            backendStorageFactory = get<BackendStorageFactory<BaseAccount>>(),
            specialFolderUpdaterFactory = get<SpecialFolderUpdater.Factory<BaseAccount>>(),
            remoteFolderCreatorFactory = get(named("imap")),
        )
    }
    factory<DomainContract.UseCase.SetArchiveFolder> {
        SetArchiveFolder(
            accountManager = get<AccountManager<BaseAccount>>(),
            backendStorageFactory = get<BackendStorageFactory<BaseAccount>>(),
            specialFolderUpdaterFactory = get<SpecialFolderUpdater.Factory<BaseAccount>>(),
        )
    }
    factory<DomainContract.UseCase.BuildSwipeActions<BaseAccount>> { parameters ->
        BuildSwipeActions(
            generalSettingsManager = get(),
            accountManager = get(),
            storage = parameters.get(),
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
        )
    }
    factory<SetupArchiveFolderDialogFragmentFactory> {
        SetupArchiveFolderDialogFragment.Factory
    }
}
