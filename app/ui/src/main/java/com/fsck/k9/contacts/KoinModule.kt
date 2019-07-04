package com.fsck.k9.contacts

import org.koin.dsl.module.applicationContext

val contactsModule = applicationContext {
    bean { ContactLetterExtractor() }
    factory { ContactLetterBitmapConfig(get(), get()) }
    factory { ContactLetterBitmapCreator(get(), get()) }
    factory { ContactPictureLoader(get(), get()) }
}
