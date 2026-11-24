package net.thunderbird.feature.mail.message.composer.internal

import net.thunderbird.feature.mail.message.composer.dialog.SentFolderNotFoundConfirmationDialogFragmentFactory
import net.thunderbird.feature.mail.message.composer.dialog.SmimeSigningDialogFragmentFactory
import net.thunderbird.feature.mail.message.composer.html.MessageComposerHtmlSettingsProvider
import net.thunderbird.feature.mail.message.composer.internal.dialog.SentFolderNotFoundConfirmationDialogFragment
import net.thunderbird.feature.mail.message.composer.internal.dialog.SmimeSigningDialogFragment
import net.thunderbird.feature.mail.message.composer.internal.html.DefaultMessageComposerHtmlSettingsProvider
import net.thunderbird.feature.mail.message.composer.internal.signature.DefaultHtmlSignatureSanitizer
import net.thunderbird.feature.mail.message.composer.internal.smime.AndroidKeyChainSmimeProvider
import net.thunderbird.feature.mail.message.composer.internal.smime.CmsSignedDataBuilder
import net.thunderbird.feature.mail.message.composer.internal.smime.DefaultSmimeSigner
import net.thunderbird.feature.mail.message.composer.signature.HtmlSignatureSanitizer
import net.thunderbird.feature.mail.message.composer.smime.SmimeKeyChainProvider
import net.thunderbird.feature.mail.message.composer.smime.SmimeSigner
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val featureMessageComposerModule = module {
    factory<SentFolderNotFoundConfirmationDialogFragmentFactory> {
        SentFolderNotFoundConfirmationDialogFragment.Factory
    }
    factory<SmimeSigningDialogFragmentFactory> {
        SmimeSigningDialogFragment.Factory
    }
    factory<MessageComposerHtmlSettingsProvider> {
        DefaultMessageComposerHtmlSettingsProvider(themeManager = get())
    }
    single<HtmlSignatureSanitizer> { DefaultHtmlSignatureSanitizer() }
    single<SmimeKeyChainProvider> {
        AndroidKeyChainSmimeProvider(
            context = androidContext(),
            logger = get(),
        )
    }
    single { CmsSignedDataBuilder() }
    single<SmimeSigner> {
        DefaultSmimeSigner(
            keyChainProvider = get(),
            cmsSignedDataBuilder = get(),
            logger = get(),
        )
    }
}
