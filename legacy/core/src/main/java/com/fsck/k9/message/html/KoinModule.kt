package com.fsck.k9.message.html

import net.thunderbird.core.common.inject.getList
import org.koin.dsl.module

val htmlModule = module {
    factory {
        HtmlProcessorFactory(
            featureFlagProvider = get(),
            cssClassNameProvider = get(),
            displayHtmlFactory = get(),
        )
    }
    factory {
        DisplayHtmlFactory(
            cssClassNameProvider = get(),
            cssStyleProviders = getList(),
        )
    }
}
