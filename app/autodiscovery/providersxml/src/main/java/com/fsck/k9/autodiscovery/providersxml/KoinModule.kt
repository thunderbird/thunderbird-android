package com.fsck.k9.autodiscovery.providersxml

import org.koin.dsl.module

val autodiscoveryProvidersXmlModule = module {
    factory { ProvidersXmlProvider(context = get()) }
    factory { ProvidersXmlDiscovery(backendManager = get(), xmlProvider = get()) }
}
