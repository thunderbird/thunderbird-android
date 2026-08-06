package net.thunderbird.core.featureflag.data

import android.content.Context
import app.cash.turbine.test
import assertk.all
import assertk.assertThat
import assertk.assertions.containsNone
import assertk.assertions.containsOnly
import assertk.assertions.isNotEmpty
import assertk.assertions.key
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.featureflag.model.AppVariantOverridesRawType
import net.thunderbird.core.featureflag.model.BaseAppVariantOverrides
import net.thunderbird.core.featureflag.serialization.FeatureFlagCatalogJsonParser
import net.thunderbird.core.featureflag.serialization.FlagRegistryOverrideSerializer
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class LocalFeatureFlagCatalogDataSourceTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `load should return the bundled feature flag catalog`() = runTest(UnconfinedTestDispatcher()) {
        val buildType = "debug"
        val k9Overrides = mapOf(
            buildType to mapOf(
                "display_in_app_notifications" to true,
                "use_notification_sender_for_system_notifications" to true,
            ),
        )
        val thunderbirdOverrides = mapOf(
            buildType to mapOf(
                "message_view_action_export_eml" to true,
            ),
        )

        // Arrange
        val registrySerializer = FlagRegistryOverrideSerializer(
            k9Factory = { FakeAppVariantOverrides(k9Overrides) },
            thunderbirdFactory = { FakeAppVariantOverrides(thunderbirdOverrides) },
        )
        val applicationContext: Context = RuntimeEnvironment.getApplication()
        val testSubject = LocalFeatureFlagCatalogDataSource(
            applicationContext = applicationContext,
            jsonParser = FeatureFlagCatalogJsonParser(registrySerializer),
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )

        // Act
        testSubject.load().test {
            val catalog = awaitItem()
            // Assert
            assertThat(catalog.flags).isNotEmpty()
            assertThat(catalog.overrides.k9).all {
                isNotEmpty()
                key(buildType).all {
                    containsOnly(elements = k9Overrides.getValue(buildType).toList().toTypedArray())
                    containsNone(elements = thunderbirdOverrides.getValue(buildType).toList().toTypedArray())
                }
            }
            assertThat(catalog.overrides.thunderbird).all {
                isNotEmpty()
                key(buildType).all {
                    containsOnly(elements = thunderbirdOverrides.getValue(buildType).toList().toTypedArray())
                    containsNone(elements = k9Overrides.getValue(buildType).toList().toTypedArray())
                }
            }
        }
    }
}

private class FakeAppVariantOverrides(wrapper: AppVariantOverridesRawType) : BaseAppVariantOverrides(wrapper)
