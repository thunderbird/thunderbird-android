package net.thunderbird.core.featureflag.data

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import net.thunderbird.core.featureflag.R
import net.thunderbird.core.featureflag.model.FeatureFlagCatalog
import net.thunderbird.core.featureflag.serialization.FeatureFlagCatalogJsonParser

/**
 * Android implementation of [FeatureFlagCatalogDataSource] that loads the feature flag catalog
 * from a local raw resource file.
 *
 * Reads the catalog JSON file from the application's raw resources, parses it on the IO dispatcher,
 * and returns the deserialized FeatureFlagCatalog.
 *
 * @param applicationContext Android application context for accessing resources.
 * @param jsonParser Parser for deserializing the catalog JSON into a FeatureFlagCatalog object.
 * @param ioDispatcher Coroutine dispatcher for performing IO operations, defaults to Dispatchers.IO.
 */
internal actual class LocalFeatureFlagCatalogDataSource(
    private val applicationContext: Context,
    private val jsonParser: FeatureFlagCatalogJsonParser,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : FeatureFlagCatalogDataSource {
    actual override fun load(): Flow<FeatureFlagCatalog> = flow {
        val text = applicationContext.resources
            .openRawResource(R.raw.thunderbird_mobile_featureflag_catalog)
            .bufferedReader()
            .use { reader -> reader.readText() }
        emit(jsonParser.decodeFromString(text))
    }.flowOn(ioDispatcher)
}
