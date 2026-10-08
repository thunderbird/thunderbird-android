package net.thunderbird.core.featureflag.inject

import com.eygraber.uri.toKmpUri
import net.thunderbird.core.featureflag.config.BuildConfig
import net.thunderbird.core.featureflag.data.FeatureFlagCatalogDataSource
import net.thunderbird.core.featureflag.data.LocalFeatureFlagCatalogDataSource
import net.thunderbird.core.featureflag.data.RemoteFeatureFlagCatalogDataSource
import net.thunderbird.core.featureflag.inject.qualifier.FEATURE_FLAG_JSON_QUALIFIER
import net.thunderbird.core.featureflag.inject.qualifier.InjectQualifier
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.core.qualifier.qualifier
import org.koin.dsl.module

actual val platformFeatureFlagModule: Module = module {
    single<FeatureFlagCatalogDataSource>(qualifier(InjectQualifier.Local)) {
        LocalFeatureFlagCatalogDataSource(
            applicationContext = androidApplication(),
            jsonParser = get(),
        )
    }

    single<FeatureFlagCatalogDataSource>(qualifier(InjectQualifier.Remote)) {
        val context = androidContext()
        RemoteFeatureFlagCatalogDataSource(
            url = BuildConfig.FEATURE_FLAG_REMOTE_URL,
            cacheFileUri =
            "file://${context.filesDir.path}/${BuildConfig.FEATURE_FLAG_REMOTE_CACHE_FILENAME}".toKmpUri(),
            logger = get(),
            configStore = get(),
            fileSystemManager = get(),
            json = get(named(FEATURE_FLAG_JSON_QUALIFIER)),
        )
    }
}
