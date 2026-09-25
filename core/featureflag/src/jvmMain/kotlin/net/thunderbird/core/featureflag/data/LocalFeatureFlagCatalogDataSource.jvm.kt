package net.thunderbird.core.featureflag.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import net.thunderbird.core.featureflag.model.FeatureFlagCatalog
import net.thunderbird.core.featureflag.serialization.FeatureFlagCatalogJsonParser

private const val CATALOG_RESOURCE_PATH = "thunderbird_mobile_featureflag_catalog.json"

internal actual class LocalFeatureFlagCatalogDataSource(
    private val jsonParser: FeatureFlagCatalogJsonParser,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : FeatureFlagCatalogDataSource {
    actual override fun observe(): Flow<FeatureFlagCatalog> = flow {
        emit(load())
    }

    actual override suspend fun load(): FeatureFlagCatalog = withContext(ioDispatcher) {
        val stream = LocalFeatureFlagCatalogDataSource::class.java.classLoader
            ?.getResourceAsStream(CATALOG_RESOURCE_PATH)
            ?: error("Feature flag catalog '$CATALOG_RESOURCE_PATH' not found on the classpath")
        val text = stream.bufferedReader().use { reader -> reader.readText() }
        jsonParser.decodeFromString(text)
    }
}
