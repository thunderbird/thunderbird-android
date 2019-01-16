package com.fsck.k9.activity

import com.fsck.k9.autodiscovery.*
import org.koin.dsl.module.applicationContext

val activityModule = applicationContext {
    bean { ColorChipProvider() }
    bean { ProvidersXmlDiscovery(get(), get()) }
    bean { ThunderbirdAutoconfigParser() }
    bean { ThunderbirdAutoconfigFetcher() }
    bean { ThunderbirdDiscovery(get(), get()) }
    bean { ServerSettingsDiscovery(arrayOf(
            get<ProvidersXmlDiscovery>(),
            get<ThunderbirdDiscovery>()
    )) }
}
