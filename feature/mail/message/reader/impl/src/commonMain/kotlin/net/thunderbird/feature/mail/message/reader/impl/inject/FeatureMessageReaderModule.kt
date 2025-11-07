package net.thunderbird.feature.mail.message.reader.impl.inject

import net.thunderbird.core.featureflag.FeatureFlagProvider
import net.thunderbird.feature.mail.message.reader.api.MessageReaderFeatureFlags
import net.thunderbird.feature.mail.message.reader.api.css.CssVariableNameProvider
import net.thunderbird.feature.mail.message.reader.api.css.GlobalCssStyleProvider
import net.thunderbird.feature.mail.message.reader.api.css.PlainTextMessagePreElementCssStyleProvider
import net.thunderbird.feature.mail.message.reader.api.css.SignatureCssStyleProvider
import net.thunderbird.feature.mail.message.reader.impl.css.DefaultCssVariableNameProvider
import net.thunderbird.feature.mail.message.reader.impl.css.DefaultGlobalCssStyleProvider
import net.thunderbird.feature.mail.message.reader.impl.css.DefaultPlainTextMessagePreElementCssStyleProvider
import net.thunderbird.feature.mail.message.reader.impl.css.DefaultSignatureCssStyleProvider
import net.thunderbird.feature.mail.message.reader.impl.css.LegacyGlobalCssStyleProvider
import org.koin.dsl.module

val featureMessageReaderModule = module {
    single<CssVariableNameProvider> { DefaultCssVariableNameProvider(get()) }
    single<GlobalCssStyleProvider> {
        val featureFlagProvider = get<FeatureFlagProvider>()
        if (featureFlagProvider.provide(MessageReaderFeatureFlags.UseNewMessageReaderCssStyles).isEnabled()) {
            DefaultGlobalCssStyleProvider(
                cssClassNameProvider = get(),
                cssVariableNameProvider = get(),
            )
        } else {
            LegacyGlobalCssStyleProvider()
        }
    }
    single<SignatureCssStyleProvider> { DefaultSignatureCssStyleProvider(get()) }
    factory<PlainTextMessagePreElementCssStyleProvider.Factory> {
        DefaultPlainTextMessagePreElementCssStyleProvider.Factory(cssClassNameProvider = get())
    }
}
