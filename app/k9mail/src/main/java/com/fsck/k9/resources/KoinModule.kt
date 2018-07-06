package com.fsck.k9.resources

import com.fsck.k9.CoreResourceProvider
import com.fsck.k9.autocrypt.AutocryptStringProvider
import org.koin.dsl.module.applicationContext

val resourcesModule = applicationContext {
    bean { K9CoreResourceProvider(get()) as CoreResourceProvider }
    bean { K9AutocryptStringProvider(get()) as AutocryptStringProvider}
}
