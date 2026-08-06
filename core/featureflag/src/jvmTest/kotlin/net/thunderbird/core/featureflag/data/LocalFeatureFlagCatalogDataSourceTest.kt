package net.thunderbird.core.featureflag.data

import kotlinx.coroutines.CoroutineDispatcher
import net.thunderbird.core.featureflag.serialization.FeatureFlagCatalogJsonParser

internal class LocalFeatureFlagCatalogDataSourceTest : BaseLocalFeatureFlagCatalogDataSourceTest() {
    override fun createDataSource(
        jsonParser: FeatureFlagCatalogJsonParser,
        ioDispatcher: CoroutineDispatcher,
    ): LocalFeatureFlagCatalogDataSource = LocalFeatureFlagCatalogDataSource(
        jsonParser = jsonParser,
        ioDispatcher = ioDispatcher,
    )
}
