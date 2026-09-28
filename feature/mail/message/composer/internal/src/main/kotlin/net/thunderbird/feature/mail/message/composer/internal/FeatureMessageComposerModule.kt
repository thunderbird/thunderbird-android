package net.thunderbird.feature.mail.message.composer.internal

import net.thunderbird.feature.mail.message.composer.dialog.SentFolderNotFoundConfirmationDialogFragmentFactory
import net.thunderbird.feature.mail.message.composer.html.MessageComposerHtmlSettingsProvider
import net.thunderbird.feature.mail.message.composer.internal.dialog.SentFolderNotFoundConfirmationDialogFragment
import net.thunderbird.feature.mail.message.composer.internal.html.DefaultMessageComposerHtmlSettingsProvider
import net.thunderbird.feature.mail.message.composer.internal.signature.DefaultHtmlSignatureSanitizer
import net.thunderbird.feature.mail.message.composer.signature.HtmlSignatureSanitizer
import org.koin.dsl.module

val featureMessageComposerModule = module {
    factory<SentFolderNotFoundConfirmationDialogFragmentFactory> {
        SentFolderNotFoundConfirmationDialogFragment.Factory
    }
    factory<MessageComposerHtmlSettingsProvider> {
        DefaultMessageComposerHtmlSettingsProvider(themeManager = get())
    }
    single<HtmlSignatureSanitizer> { DefaultHtmlSignatureSanitizer() }
}
