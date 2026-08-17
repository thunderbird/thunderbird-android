package net.thunderbird.core.featureflag.data

import app.cash.turbine.test
import assertk.all
import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.containsOnly
import assertk.assertions.hasMessage
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import assertk.assertions.isTrue
import assertk.assertions.key
import assertk.assertions.prop
import kotlin.test.Test
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import net.thunderbird.core.featureflag.model.AppVariantOverridesRawType
import net.thunderbird.core.featureflag.model.BaseAppVariantOverrides
import net.thunderbird.core.featureflag.model.FeatureFlagCatalog
import net.thunderbird.core.featureflag.model.FlagAttributeType
import net.thunderbird.core.featureflag.model.FlagRegistry
import net.thunderbird.core.featureflag.serialization.DefaultFeatureFlagCatalogJsonParser
import net.thunderbird.core.featureflag.serialization.FeatureFlagCatalogJsonParser
import net.thunderbird.core.featureflag.serialization.FlagRegistryOverrideSerializer

/**
 * Shared expectations for every [LocalFeatureFlagCatalogDataSource] actual.
 *
 * The actual constructors differ per target — Android needs an application `Context`, the JVM does not — and the
 * Android target additionally requires Robolectric's JUnit runner, which cannot be expressed in common code. Each
 * target therefore contributes a concrete subclass that only supplies [createDataSource]. JUnit does not run abstract
 * classes, so this class never executes without a platform binding.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal abstract class BaseLocalFeatureFlagCatalogDataSourceTest {

    /**
     * Creates the platform actual of [LocalFeatureFlagCatalogDataSource].
     */
    protected abstract fun createDataSource(
        jsonParser: FeatureFlagCatalogJsonParser,
        ioDispatcher: CoroutineDispatcher,
    ): LocalFeatureFlagCatalogDataSource

    @Test
    fun `load should return the bundled feature flag catalog`() = runTest(UnconfinedTestDispatcher()) {
        // Arrange
        val testSubject = createTestSubject()

        // Act
        testSubject.load().test {
            val catalog = awaitItem()

            // Assert
            assertThat(catalog.version).isEqualTo(CATALOG_VERSION)
            assertThat(catalog.flags).hasSize(CATALOG_FLAG_COUNT)
        }
    }

    @Test
    fun `load should deserialize the override section of both applications`() = runTest(UnconfinedTestDispatcher()) {
        // Arrange
        val testSubject = createTestSubject()

        // Act
        testSubject.load().test {
            val catalog = awaitItem()

            // Assert
            assertThat(catalog.overrides.thunderbird.keys)
                .containsExactlyInAnyOrder("debug", "daily", "beta", "release")
            assertThat(catalog.overrides.k9.keys).containsExactlyInAnyOrder("debug", "release")
        }
    }

    @Test
    fun `load should deserialize representative thunderbird and k9 overrides`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            val testSubject = createTestSubject()

            // Act
            testSubject.load().test {
                val catalog = awaitItem()

                // Assert
                assertThat(catalog.overrides.thunderbird).all {
                    key("debug").containsOnly(
                        "display_in_app_notifications" to true,
                        "use_notification_sender_for_system_notifications" to true,
                        "message_view_action_export_eml" to true,
                    )
                    key("daily").containsOnly(
                        "display_in_app_notifications" to true,
                        "message_view_action_export_eml" to true,
                    )
                    key("beta").containsOnly("display_in_app_notifications" to true)
                    key("release").isEmpty()
                }
                assertThat(catalog.overrides.k9).all {
                    key("debug").containsOnly(
                        "display_in_app_notifications" to true,
                        "use_notification_sender_for_system_notifications" to true,
                        "message_view_action_export_eml" to true,
                    )
                    key("release").isEmpty()
                }
            }
        }

    @Test
    fun `load should fall back to flag defaults when optional fields are omitted`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            val testSubject = createTestSubject()

            // Act
            testSubject.load().test {
                val catalog = awaitItem()

                // Assert
                assertThat(catalog.flags.first { it.key == "archive_marks_as_read" }).all {
                    prop(FlagRegistry::default).isTrue()
                    prop(FlagRegistry::type).isEqualTo(FlagAttributeType.Boolean)
                    prop(FlagRegistry::timeToPromote).isNull()
                }
                assertThat(catalog.flags.first { it.key == "use_new_message_reader_css_styles" })
                    .prop(FlagRegistry::timeToPromote)
                    .isEqualTo("2026-12-31")
            }
        }

    @Test
    fun `load should surface the parser failure when the catalog json is malformed`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            val parserFailure = SerializationException(MALFORMED_CATALOG_ERROR_MESSAGE)
            val testSubject = createTestSubject(
                jsonParser = FakeFeatureFlagCatalogJsonParser(Result.failure(parserFailure)),
            )

            // Act
            testSubject.load().test {
                // Assert
                assertThat(awaitError()).all {
                    isInstanceOf<SerializationException>()
                    hasMessage(MALFORMED_CATALOG_ERROR_MESSAGE)
                }
            }
        }

    private fun TestScope.createTestSubject(
        jsonParser: FeatureFlagCatalogJsonParser = createJsonParser(),
    ): LocalFeatureFlagCatalogDataSource = createDataSource(
        jsonParser = jsonParser,
        ioDispatcher = StandardTestDispatcher(testScheduler),
    )

    private companion object {
        const val CATALOG_VERSION = "2026-07-30.1"
        const val CATALOG_FLAG_COUNT = 12
        const val MALFORMED_CATALOG_ERROR_MESSAGE =
            "Unexpected JSON token at offset 42: Expected quotation mark '\"', but had '}' instead"
    }
}

private fun createJsonParser(): FeatureFlagCatalogJsonParser = DefaultFeatureFlagCatalogJsonParser(
    registrySerializer = FlagRegistryOverrideSerializer(
        k9Factory = { wrapper -> FakeAppVariantOverrides(wrapper) },
        thunderbirdFactory = { wrapper -> FakeAppVariantOverrides(wrapper) },
    ),
)

private class FakeFeatureFlagCatalogJsonParser(
    private val result: Result<FeatureFlagCatalog>,
) : FeatureFlagCatalogJsonParser {
    override fun decodeFromString(rawJson: String): FeatureFlagCatalog = result.getOrThrow()
}

/**
 * Passes the deserialized overrides through unchanged so the assertions run against what the catalog JSON actually
 * declares, instead of a hard-coded map.
 */
private class FakeAppVariantOverrides(wrapper: AppVariantOverridesRawType) : BaseAppVariantOverrides(wrapper)
