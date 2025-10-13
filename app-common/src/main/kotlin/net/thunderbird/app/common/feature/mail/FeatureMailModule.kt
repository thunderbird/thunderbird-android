package net.thunderbird.app.common.feature.mail

import com.fsck.k9.mailstore.DefaultSpecialFolderUpdater
import com.fsck.k9.mailstore.LegacyAccountDtoSpecialFolderUpdaterFactory
import net.thunderbird.app.common.feature.mail.message.mailMessageModule
import net.thunderbird.backend.api.BackendFactory
import net.thunderbird.backend.api.BackendStorageFactory
import net.thunderbird.backend.api.folder.RemoteFolderCreator
import net.thunderbird.backend.imap.DefaultImapRemoteFolderCreatorFactory
import net.thunderbird.backend.imap.ImapRemoteFolderCreatorFactory
import net.thunderbird.feature.mail.account.api.BaseAccount
import net.thunderbird.feature.mail.folder.api.SpecialFolderUpdater
import org.koin.dsl.module

internal val appCommonFeatureMailModule = module {

    includes(mailMessageModule)

    single<BackendStorageFactory<BaseAccount>> {
        BaseAccountBackendStorageFactory(
            legacyFactory = get(),
            legacyMapper = get(),
        )
    }

    factory<LegacyAccountDtoSpecialFolderUpdaterFactory> {
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

    single<BackendFactory<BaseAccount>> {
        BaseAccountImapBackendFactory(
            legacyFactory = get(),
            legacyMapper = get(),
        )
    }

    single<ImapRemoteFolderCreatorFactory> {
        DefaultImapRemoteFolderCreatorFactory(
            logger = get(),
            backendFactory = get(),
        )
    }

    single<RemoteFolderCreator.Factory> {
        RemoteFolderCreatorResolver(
            imapFactory = get(),
        )
    }
}
