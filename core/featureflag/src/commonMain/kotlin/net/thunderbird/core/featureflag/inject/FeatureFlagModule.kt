package net.thunderbird.core.featureflag.inject

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import net.thunderbird.core.configstore.ConfigId
import net.thunderbird.core.featureflag.data.configstore.DefaultFeatureFlagConfigStore
import net.thunderbird.core.featureflag.data.configstore.FeatureFlagConfigStore
import net.thunderbird.core.featureflag.inject.qualifier.FEATURE_FLAG_JSON_QUALIFIER
import net.thunderbird.core.featureflag.inject.qualifier.InjectQualifier
import net.thunderbird.core.featureflag.model.FlagRegistryOverride
import net.thunderbird.core.featureflag.provider.BundledCatalogFeatureFlagProvider
import net.thunderbird.core.featureflag.provider.BundledFeatureFlagDefaults
import net.thunderbird.core.featureflag.provider.CatalogFeatureFlagProvider
import net.thunderbird.core.featureflag.provider.evaluator.DefaultMultiFeatureFlagProviderEvaluator
import net.thunderbird.core.featureflag.provider.evaluator.MultiFeatureFlagProviderEvaluator
import net.thunderbird.core.featureflag.serialization.DefaultFeatureFlagCatalogJsonParser
import net.thunderbird.core.featureflag.serialization.FeatureFlagCatalogJsonParser
import net.thunderbird.core.featureflag.serialization.FlagRegistryOverrideSerializer
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

val featureFlagModule = module {
    single(named(FEATURE_FLAG_JSON_QUALIFIER)) {
        Json {
            serializersModule = SerializersModule {
                contextual(kClass = FlagRegistryOverride::class, serializer = get<FlagRegistryOverrideSerializer>())
            }

            ignoreUnknownKeys = false
        }
    }
    factory<FeatureFlagCatalogJsonParser> {
        DefaultFeatureFlagCatalogJsonParser(
            json = get(named(FEATURE_FLAG_JSON_QUALIFIER)),
        )
    }
    single<FeatureFlagConfigStore> {
        DefaultFeatureFlagConfigStore(
            id = ConfigId(backend = "feature_flag", feature = "storage"),
            provider = get(),
        )
    }
    single {
        BundledCatalogFeatureFlagProvider(
            dataSource = get(named(InjectQualifier.Local)),
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
