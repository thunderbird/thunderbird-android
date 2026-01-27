package net.thunderbird.feature.mail.message.reader.impl.inject

import net.thunderbird.core.common.inject.factoryListOf
import net.thunderbird.core.featureflag.FeatureFlagProvider
import net.thunderbird.feature.mail.message.reader.api.MessageReaderFeatureFlags
import net.thunderbird.feature.mail.message.reader.api.css.CssVariableNameProvider
import net.thunderbird.feature.mail.message.reader.api.css.GlobalCssStyleProvider
import net.thunderbird.feature.mail.message.reader.api.css.PlainTextMessagePreElementCssStyleProvider
import net.thunderbird.feature.mail.message.reader.api.css.SignatureCssStyleProvider
import net.thunderbird.feature.mail.message.reader.api.html.MessageReaderHtmlSettingsProvider
import net.thunderbird.feature.mail.message.reader.impl.css.DefaultCssVariableNameProvider
import net.thunderbird.feature.mail.message.reader.impl.css.DefaultGlobalCssStyleProvider
import net.thunderbird.feature.mail.message.reader.impl.css.DefaultPlainTextMessagePreElementCssStyleProvider
import net.thunderbird.feature.mail.message.reader.impl.css.DefaultSignatureCssStyleProvider
import net.thunderbird.feature.mail.message.reader.impl.css.LegacyGlobalCssStyleProvider
import net.thunderbird.feature.mail.message.reader.impl.html.DefaultMessageReaderHtmlSettingsProvider
import org.koin.dsl.module

val featureMessageReaderModule = module {
    factory<MessageReaderHtmlSettingsProvider> { DefaultMessageReaderHtmlSettingsProvider(get(), get()) }
    single<CssVariableNameProvider> { DefaultCssVariableNameProvider(get()) }
    factory<GlobalCssStyleProvider.Factory> {
        val featureFlagProvider = get<FeatureFlagProvider>()
        if (featureFlagProvider.provide(MessageReaderFeatureFlags.UseNewMessageReaderCssStyles).isEnabled()) {
            DefaultGlobalCssStyleProvider.Factory(
                cssClassNameProvider = get(),
                cssVariableNameProvider = get(),
            )
        } else {
            LegacyGlobalCssStyleProvider.Factory()
        }
    }
    factory<SignatureCssStyleProvider.Factory> { DefaultSignatureCssStyleProvider.Factory(get()) }
    factory<PlainTextMessagePreElementCssStyleProvider.Factory> {
        DefaultPlainTextMessagePreElementCssStyleProvider.Factory(cssClassNameProvider = get())
    }
    factoryListOf(
        { get<GlobalCssStyleProvider.Factory>() },
        { get<SignatureCssStyleProvider.Factory>() },
        { get<PlainTextMessagePreElementCssStyleProvider.Factory>() },
    )
}
