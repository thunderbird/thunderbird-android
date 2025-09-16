package net.thunderbird.app.common.feature.mail

import com.fsck.k9.mailstore.DefaultSpecialFolderUpdater
import net.thunderbird.backend.api.BackendFactory
import net.thunderbird.backend.api.BackendStorageFactory
import net.thunderbird.backend.api.folder.RemoteFolderCreator
import net.thunderbird.backend.imap.ImapRemoteFolderCreatorFactory
import net.thunderbird.feature.mail.account.api.BaseAccount
import net.thunderbird.feature.mail.folder.api.SpecialFolderUpdater
import org.koin.core.qualifier.named
import org.koin.dsl.module

internal val appCommonFeatureMailModule = module {

    single<BackendStorageFactory<BaseAccount>> {
        BaseAccountBackendStorageFactory(
            legacyFactory = get(),
            legacyMapper = get(),
        )
    }

    factory {
        DefaultSpecialFolderUpdater.Factory(
            folderRepository = get(),
            specialFolderSelectionStrategy = get(),
            preferences = get(),
        )
    }

    factory<SpecialFolderUpdater.Factory<BaseAccount>> {
        BaseAccountSpecialFolderUpdaterFactory(
            legacyFactory = get(),
            legacyMapper = get(),
        )
    }

    single<BackendFactory<BaseAccount>>(named("imap")) {
        BaseAccountImapBackendFactory(
            legacyFactory = get(),
            legacyMapper = get(),
        )
    }

    single<RemoteFolderCreator.Factory>(named("imap")) {
        ImapRemoteFolderCreatorFactory(
            logger = get(),
            backendFactory = get(named("imap")),
        )
    }
}
