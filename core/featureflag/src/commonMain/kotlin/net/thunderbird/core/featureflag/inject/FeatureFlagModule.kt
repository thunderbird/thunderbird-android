package net.thunderbird.core.featureflag.inject

import net.thunderbird.core.configstore.ConfigId
import net.thunderbird.core.featureflag.data.FeatureFlagCatalogDataSource
import net.thunderbird.core.featureflag.data.configstore.FeatureFlagConfigStore
import net.thunderbird.core.featureflag.provider.BaseCatalogFeatureFlagProvider
import net.thunderbird.core.featureflag.provider.BundledCatalogFeatureFlagProvider
import net.thunderbird.core.featureflag.provider.BundledFeatureFlagDefaults
import net.thunderbird.core.featureflag.provider.DataSourceCatalogFeatureFlagProvider
import net.thunderbird.core.featureflag.provider.ProviderMetadata
import net.thunderbird.core.featureflag.serialization.DefaultFeatureFlagCatalogJsonParser
import net.thunderbird.core.featureflag.serialization.FeatureFlagCatalogJsonParser
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.plugin.module.dsl.bind

val featureFlagModule = module {
    factory<FeatureFlagCatalogJsonParser> { DefaultFeatureFlagCatalogJsonParser(registrySerializer = get()) }
    single {
        val metadata = get<ProviderMetadata>()
        FeatureFlagConfigStore(
            id = ConfigId(backend = "feature_flag", feature = metadata.name),
            provider = get(),
        )
    }
    single {
        BundledCatalogFeatureFlagProvider(
            dataSource = get<FeatureFlagCatalogDataSource>(
                named(FeatureFlagCatalogDataSource.InjectQualifiers.Local),
            ),
            logger = get(),
        )
    }.bind(DataSourceCatalogFeatureFlagProvider::class)
    single<BundledFeatureFlagDefaults> { get<BundledCatalogFeatureFlagProvider>() }
    single<BaseCatalogFeatureFlagProvider> {
        // TODO(#11332): Later replaced by MultiFeatureFlagProviderEvaluator
        get<BundledCatalogFeatureFlagProvider>()
    }
    single<ProviderMetadata> { get<BaseCatalogFeatureFlagProvider>().metadata }
    includes(platformFeatureFlagModule)
}

expect val platformFeatureFlagModule: Module
