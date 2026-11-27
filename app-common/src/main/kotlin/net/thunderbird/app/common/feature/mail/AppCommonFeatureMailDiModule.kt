package net.thunderbird.app.common.feature.mail

import com.fsck.k9.backends.ImapBackendFactory
import com.fsck.k9.mailstore.DefaultSpecialFolderUpdater
import net.thunderbird.app.common.feature.mail.message.mailMessageDiModule
import net.thunderbird.backend.api.folder.RemoteFolderCreator
import net.thunderbird.backend.imap.DefaultImapRemoteFolderCreatorFactory
import net.thunderbird.backend.imap.ImapRemoteFolderCreatorFactory
import net.thunderbird.feature.mail.folder.api.SpecialFolderUpdater
import org.koin.core.qualifier.named
import org.koin.dsl.module

internal val appCommonFeatureMailDiModule = module {

    includes(mailMessageDiModule)

    factory<SpecialFolderUpdater.Factory> {
        DefaultSpecialFolderUpdater.Factory(
            accountManager = get(),
            remoteFolderQueryRepository = get(),
            folderDetailsRepository = get(),
            specialFolderSelectionStrategy = get(),
            coroutineScope = get(named("AppCoroutineScope")),
            logger = get(),
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
