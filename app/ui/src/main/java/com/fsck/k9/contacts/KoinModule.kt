package com.fsck.k9.contacts

import org.koin.dsl.module

val contactsModule = module {
    single { ContactLetterExtractor() }
    factory { ContactLetterBitmapConfig(get(), get()) }
    factory { ContactLetterBitmapCreator(get(), get()) }
    factory { ContactPictureLoader(get(), get()) }
}
