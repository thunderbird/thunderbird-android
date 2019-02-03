package com.fsck.k9.resources

import com.fsck.k9.CoreResourceProvider
import com.fsck.k9.autocrypt.AutocryptStringProvider
import org.koin.dsl.module.module

val resourcesModule = module {
    single { K9CoreResourceProvider(get()) as CoreResourceProvider }
    single { K9AutocryptStringProvider(get()) as AutocryptStringProvider}
}
