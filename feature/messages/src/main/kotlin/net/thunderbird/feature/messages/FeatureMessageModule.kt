package net.thunderbird.feature.messages

import net.thunderbird.backend.api.BackendStorageFactory
import net.thunderbird.feature.mail.account.api.AccountManager
import net.thunderbird.feature.mail.account.api.BaseAccount
import net.thunderbird.feature.mail.folder.api.SpecialFolderUpdater
import net.thunderbird.feature.messages.domain.DomainContract
import net.thunderbird.feature.messages.domain.usecase.CreateArchiveFolder
import net.thunderbird.feature.messages.domain.usecase.GetAccountFolders
import net.thunderbird.feature.messages.domain.usecase.SetArchiveFolder
import net.thunderbird.feature.messages.ui.dialog.SetupArchiveFolderDialogFragment
import net.thunderbird.feature.messages.ui.dialog.SetupArchiveFolderDialogFragmentFactory
import net.thunderbird.feature.messages.ui.dialog.SetupArchiveFolderDialogViewModel
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
            baseAccountManager = get<AccountManager<BaseAccount>>(),
            backendStorageFactory = get<BackendStorageFactory<BaseAccount>>(),
            specialFolderUpdaterFactory = get<SpecialFolderUpdater.Factory<BaseAccount>>(),
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
        )
    }
    factory<SetupArchiveFolderDialogFragmentFactory> {
        SetupArchiveFolderDialogFragment.Factory
    }
}
