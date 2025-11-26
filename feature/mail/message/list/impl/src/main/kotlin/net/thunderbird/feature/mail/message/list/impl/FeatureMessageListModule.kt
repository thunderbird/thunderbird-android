package net.thunderbird.feature.mail.message.list.impl

import net.thunderbird.feature.mail.account.api.BaseAccount
import net.thunderbird.feature.mail.message.list.api.domain.DomainContract
import net.thunderbird.feature.mail.message.list.api.ui.dialog.SetupArchiveFolderDialogContract
import net.thunderbird.feature.mail.message.list.api.ui.dialog.SetupArchiveFolderDialogFragmentFactory
import net.thunderbird.feature.mail.message.list.impl.domain.usecase.BuildSwipeActions
import net.thunderbird.feature.mail.message.list.impl.domain.usecase.CreateArchiveFolder
import net.thunderbird.feature.mail.message.list.impl.domain.usecase.GetAccountFolders
import net.thunderbird.feature.mail.message.list.impl.domain.usecase.SetArchiveFolder
import net.thunderbird.feature.mail.message.list.impl.ui.dialog.SetupArchiveFolderDialogFragment
import net.thunderbird.feature.mail.message.list.impl.ui.dialog.SetupArchiveFolderDialogViewModel
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
    factory<DomainContract.UseCase.BuildSwipeActions<BaseAccount>> { parameters ->
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
}
