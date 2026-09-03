package net.thunderbird.core.featureflag.inject

import net.thunderbird.core.configstore.ConfigId
import net.thunderbird.core.featureflag.data.FeatureFlagCatalogDataSource
import net.thunderbird.core.featureflag.data.configstore.DefaultFeatureFlagConfigStore
import net.thunderbird.core.featureflag.data.configstore.FeatureFlagConfigStore
import net.thunderbird.core.featureflag.inject.qualifier.InjectQualifier
import net.thunderbird.core.featureflag.provider.BundledCatalogFeatureFlagProvider
import net.thunderbird.core.featureflag.provider.BundledFeatureFlagDefaults
import net.thunderbird.core.featureflag.provider.CatalogFeatureFlagProvider
import net.thunderbird.core.featureflag.provider.evaluator.DefaultMultiFeatureFlagProviderEvaluator
import net.thunderbird.core.featureflag.provider.evaluator.MultiFeatureFlagProviderEvaluator
import net.thunderbird.core.featureflag.serialization.DefaultFeatureFlagCatalogJsonParser
import net.thunderbird.core.featureflag.serialization.FeatureFlagCatalogJsonParser
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

val featureFlagModule = module {
    factory<FeatureFlagCatalogJsonParser> { DefaultFeatureFlagCatalogJsonParser(registrySerializer = get()) }
    single<FeatureFlagConfigStore> {
        DefaultFeatureFlagConfigStore(
            id = ConfigId(backend = "feature_flag", feature = "storage"),
            provider = get(),
        )
    }
    single {
        BundledCatalogFeatureFlagProvider(
            dataSource = get<FeatureFlagCatalogDataSource>(
                named(InjectQualifier.Local),
            ),
            logger = get(),
        )
    }
    single<CatalogFeatureFlagProvider>(named(InjectQualifier.Local)) {
        get<BundledCatalogFeatureFlagProvider>()
    }
    single<BundledFeatureFlagDefaults> {
        get<BundledCatalogFeatureFlagProvider>()
    }
    single<MultiFeatureFlagProviderEvaluator> {
        DefaultMultiFeatureFlagProviderEvaluator(
            providers = buildList {
                getOrNull<CatalogFeatureFlagProvider>(named(InjectQualifier.InMemory))
                    ?.let { add(it) }
                // add(get<CatalogFeatureFlagProvider>(named(InjectQualifier.Remote)))
                add(get<CatalogFeatureFlagProvider>(named(InjectQualifier.Local)))
            },
            logger = get(),
        )
    }
    includes(platformFeatureFlagModule)
}

expect val platformFeatureFlagModule: Module
