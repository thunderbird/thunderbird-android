package net.thunderbird.app.common.feature.mail

import com.fsck.k9.backends.ImapBackendFactory
import com.fsck.k9.mailstore.DefaultSpecialFolderUpdater
import net.thunderbird.app.common.feature.mail.message.mailMessageModule
import net.thunderbird.backend.api.folder.RemoteFolderCreator
import net.thunderbird.backend.imap.DefaultImapRemoteFolderCreatorFactory
import net.thunderbird.backend.imap.ImapRemoteFolderCreatorFactory
import net.thunderbird.feature.mail.folder.api.SpecialFolderUpdater
import org.koin.core.qualifier.named
import org.koin.dsl.module

internal val appCommonFeatureMailModule = module {

    includes(mailMessageModule)

    factory<SpecialFolderUpdater.Factory> {
        DefaultSpecialFolderUpdater.Factory(
            accountManager = get(),
            folderRepository = get(),
            specialFolderSelectionStrategy = get(),
            coroutineScope = get(named("AppCoroutineScope")),
        )
    }

    single<ImapRemoteFolderCreatorFactory> {
        DefaultImapRemoteFolderCreatorFactory(
            logger = get(),
            backendFactory = get<ImapBackendFactory>(),
        )
    }

    single<RemoteFolderCreator.Factory> {
        RemoteFolderCreatorResolver(
            accountManager = get(),
            imapFactory = get(),
        )
    }
}
