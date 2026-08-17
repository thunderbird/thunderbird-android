package net.thunderbird.core.featureflag.inject

import net.thunderbird.core.featureflag.serialization.DefaultFeatureFlagCatalogJsonParser
import net.thunderbird.core.featureflag.serialization.FeatureFlagCatalogJsonParser
import org.koin.dsl.module

val featureFlagModule = module {
    factory<FeatureFlagCatalogJsonParser> { DefaultFeatureFlagCatalogJsonParser(registrySerializer = get()) }
}
