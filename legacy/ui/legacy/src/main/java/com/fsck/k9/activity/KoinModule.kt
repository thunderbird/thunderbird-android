package com.fsck.k9.activity

import app.k9mail.feature.launcher.FeatureLauncherExternalContract
import com.fsck.k9.ui.messageview.AttachmentLoadingController
import com.fsck.k9.ui.messageview.DefaultAttachmentLoadingController
import org.koin.dsl.module

val activityModule = module {
    single {
        MessageLoaderHelperFactory(
            messageViewInfoExtractorFactory = get(),
            messageReaderHtmlSettingsProvider = get(),
            messageComposerHtmlSettingsProvider = get(),
        )
    }
    factory<AttachmentLoadingController> {
        DefaultAttachmentLoadingController(messagingController = get(), accountManager = get())
    }
    single<FeatureLauncherExternalContract.AccountManageIdentitiesLauncher> {
        DefaultAccountManageIdentitiesLauncher()
    }
}
