package net.thunderbird.core.featureflag.inject

import net.thunderbird.core.featureflag.data.FeatureFlagCatalogDataSource
import net.thunderbird.core.featureflag.data.LocalFeatureFlagCatalogDataSource
import org.koin.core.module.Module
import org.koin.core.qualifier.qualifier
import org.koin.dsl.module

actual val platformFeatureFlagModule: Module = module {
    single<FeatureFlagCatalogDataSource>(qualifier(FeatureFlagCatalogDataSource.InjectQualifiers.Local)) {
        LocalFeatureFlagCatalogDataSource(jsonParser = get())
    }
}
