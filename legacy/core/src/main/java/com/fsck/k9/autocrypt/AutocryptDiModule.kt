package com.fsck.k9.autocrypt

import org.koin.dsl.module

internal val autocryptDiModule = module {
    single {
        AutocryptTransferMessageCreator(
            stringProvider = get(),
            generalSettingsManager = get(),
        )
    }
    single { AutocryptDraftStateHeaderParser() }
}
