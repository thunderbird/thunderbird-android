package com.fsck.k9.ui

import android.content.Context
import app.k9mail.legacy.message.controller.MessagingControllerMailChecker
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.ui.helper.DisplayHtmlUiFactory
import com.fsck.k9.ui.helper.SizeFormatter
import com.fsck.k9.ui.messageview.LinkTextHandler
import com.fsck.k9.ui.share.ShareIntentBuilder
import net.thunderbird.core.common.inject.getList
import org.koin.core.qualifier.named
import org.koin.dsl.module

val uiModule = module {
    factory {
        DisplayHtmlUiFactory(
            cssClassNameProvider = get(),
            cssStyleProviders = getList(),
            messageReaderHtmlSettingsProvider = get(),
            messageComposerHtmlSettingsProvider = get(),
        )
    }
    single<MessagingControllerMailChecker> { get<MessagingController>() }
    factory(named("MessageView")) { get<DisplayHtmlUiFactory>().createForMessageView() }
    factory { (context: Context) -> SizeFormatter(context.resources) }
    factory { ShareIntentBuilder(resourceProvider = get(), textPartFinder = get(), quoteDateFormatter = get()) }
    factory { LinkTextHandler(context = get(), clipboardManager = get()) }
}
