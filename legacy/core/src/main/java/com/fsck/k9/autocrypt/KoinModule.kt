package com.fsck.k9.autocrypt

import org.koin.dsl.module

val autocryptModule = module {
    single {
        AutocryptTransferMessageCreator(
            stringProvider = get(),
            generalSettingsManager = get(),
        )
    }
    single { AutocryptDraftStateHeaderParser() }
}
