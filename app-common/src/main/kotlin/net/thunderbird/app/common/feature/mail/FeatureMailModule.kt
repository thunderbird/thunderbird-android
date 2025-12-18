package net.thunderbird.app.common.feature.mail

import com.fsck.k9.mailstore.DefaultSpecialFolderUpdater
import net.thunderbird.app.common.feature.mail.message.mailMessageModule
import net.thunderbird.backend.api.folder.RemoteFolderCreator
import net.thunderbird.backend.imap.DefaultImapRemoteFolderCreatorFactory
import net.thunderbird.backend.imap.ImapRemoteFolderCreatorFactory
import net.thunderbird.feature.mail.folder.api.SpecialFolderUpdater
import org.koin.dsl.module

internal val appCommonFeatureMailModule = module {

    includes(mailMessageModule)

    factory<SpecialFolderUpdater.Factory> {
        DefaultSpecialFolderUpdater.Factory(
            accountManager = get(),
            folderRepository = get(),
            specialFolderSelectionStrategy = get(),
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
            accountManager = get(),
            imapFactory = get(),
        )
    }
}
