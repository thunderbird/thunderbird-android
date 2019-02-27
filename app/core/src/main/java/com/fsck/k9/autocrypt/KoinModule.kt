package com.fsck.k9.autocrypt

import org.koin.dsl.module.module

val autocryptModule = module {
    single { AutocryptTransferMessageCreator(get()) }
    single { AutocryptDraftStateHeaderParser() }
}