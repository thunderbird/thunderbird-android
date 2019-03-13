package com.fsck.k9.autodiscovery

import com.fsck.k9.autodiscovery.providersxml.ProvidersXmlDiscovery
import com.fsck.k9.autodiscovery.providersxml.ProvidersXmlProvider
import org.koin.dsl.module.applicationContext

val autodiscoveryModule = applicationContext {
    factory { ProvidersXmlProvider(get()) }
    factory { ProvidersXmlDiscovery(get(), get()) }
}
