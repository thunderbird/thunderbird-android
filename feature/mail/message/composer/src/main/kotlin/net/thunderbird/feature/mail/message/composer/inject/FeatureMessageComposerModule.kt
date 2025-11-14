package net.thunderbird.feature.mail.message.composer.inject

import net.thunderbird.feature.mail.message.composer.dialog.SentFolderNotFoundConfirmationDialogFragment
import net.thunderbird.feature.mail.message.composer.dialog.SentFolderNotFoundConfirmationDialogFragmentFactory
import org.koin.dsl.module

val featureMessageComposerModule = module {
    factory<SentFolderNotFoundConfirmationDialogFragmentFactory> {
        SentFolderNotFoundConfirmationDialogFragment.Factory
    }
}
