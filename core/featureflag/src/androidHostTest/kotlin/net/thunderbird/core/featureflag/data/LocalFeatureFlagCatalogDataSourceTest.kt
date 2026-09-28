package net.thunderbird.core.featureflag.data

import kotlinx.coroutines.CoroutineDispatcher
import net.thunderbird.core.featureflag.serialization.FeatureFlagCatalogJsonParser
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
internal class LocalFeatureFlagCatalogDataSourceTest : BaseLocalFeatureFlagCatalogDataSourceTest() {
    override fun createDataSource(
        jsonParser: FeatureFlagCatalogJsonParser,
        ioDispatcher: CoroutineDispatcher,
    ): LocalFeatureFlagCatalogDataSource = LocalFeatureFlagCatalogDataSource(
        applicationContext = RuntimeEnvironment.getApplication(),
        jsonParser = jsonParser,
        ioDispatcher = ioDispatcher,
    )
}
