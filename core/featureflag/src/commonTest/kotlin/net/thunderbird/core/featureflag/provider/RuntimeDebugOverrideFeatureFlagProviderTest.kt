package net.thunderbird.core.featureflag.provider

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsOnly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.featureflag.FeatureFlagResult
import net.thunderbird.core.featureflag.data.configstore.FeatureFlagConfigData
import net.thunderbird.core.featureflag.data.configstore.FeatureFlagConfigStore
import net.thunderbird.core.featureflag.keys.GeneratedFeatureFlagKey.ARCHIVE_MARKS_AS_READ
import net.thunderbird.core.featureflag.keys.GeneratedFeatureFlagKey.DISPLAY_IN_APP_NOTIFICATIONS
import net.thunderbird.core.featureflag.keys.GeneratedFeatureFlagKey.MESSAGE_VIEW_ACTION_EXPORT_EML
import net.thunderbird.core.featureflag.model.FlagOverrides
import net.thunderbird.core.featureflag.provider.context.FeatureFlagContext
import net.thunderbird.core.featureflag.provider.context.ImmutableFeatureFlagContext
import net.thunderbird.core.logging.testing.TestLogger

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalUuidApi::class)
class RuntimeDebugOverrideFeatureFlagProviderTest {

    @Test
    fun `provide should return Enabled when the persisted override is enabled`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            val testSubject = createTestSubject(
                configStore = FakeFeatureFlagConfigStore(
                    overrides = mapOf(MESSAGE_VIEW_ACTION_EXPORT_EML.key to true),
                ),
            )

            // Act
            val result = testSubject.provide(MESSAGE_VIEW_ACTION_EXPORT_EML)

            // Assert
            assertThat(result).isEqualTo(FeatureFlagResult.Enabled)
        }

    @Test
    fun `provide should return Disabled when the persisted override is disabled`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            val testSubject = createTestSubject(
                configStore = FakeFeatureFlagConfigStore(
                    overrides = mapOf(MESSAGE_VIEW_ACTION_EXPORT_EML.key to false),
                ),
            )

            // Act
            val result = testSubject.provide(MESSAGE_VIEW_ACTION_EXPORT_EML)

            // Assert
            assertThat(result).isEqualTo(FeatureFlagResult.Disabled)
        }

    @Test
    fun `provide should return Unavailable when there is no override for the key`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            val testSubject = createTestSubject(
                configStore = FakeFeatureFlagConfigStore(
                    overrides = mapOf(ARCHIVE_MARKS_AS_READ.key to true),
                ),
            )

            // Act
            val result = testSubject.provide(MESSAGE_VIEW_ACTION_EXPORT_EML)

            // Assert
            assertThat(result).isEqualTo(FeatureFlagResult.Unavailable)
        }

    @Test
    fun `setOverride should persist the override and expose it through provide`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            val configStore = FakeFeatureFlagConfigStore()
            val testSubject = createTestSubject(configStore)

            // Act
            testSubject.setOverride(key = MESSAGE_VIEW_ACTION_EXPORT_EML, enabled = true)

            // Assert
            testSubject.overrides.test {
                val overrides = awaitItem()
                assertThat(configStore.current.overrides).containsOnly(MESSAGE_VIEW_ACTION_EXPORT_EML.key to true)
                assertThat(overrides).containsOnly(MESSAGE_VIEW_ACTION_EXPORT_EML.key to true)
                assertThat(testSubject.provide(MESSAGE_VIEW_ACTION_EXPORT_EML)).isEqualTo(FeatureFlagResult.Enabled)
            }
        }

    @Test
    fun `setOverride should replace an existing override for the same key`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            val configStore = FakeFeatureFlagConfigStore(
                overrides = mapOf(MESSAGE_VIEW_ACTION_EXPORT_EML.key to true),
            )
            val testSubject = createTestSubject(configStore)

            // Act
            testSubject.setOverride(key = MESSAGE_VIEW_ACTION_EXPORT_EML, enabled = false)

            // Assert
            assertThat(configStore.current.overrides).containsOnly(MESSAGE_VIEW_ACTION_EXPORT_EML.key to false)
            assertThat(testSubject.provide(MESSAGE_VIEW_ACTION_EXPORT_EML)).isEqualTo(FeatureFlagResult.Disabled)
        }

    @Test
    fun `clearOverride should remove only the given override`() = runTest(UnconfinedTestDispatcher()) {
        // Arrange
        val configStore = FakeFeatureFlagConfigStore(
            overrides = mapOf(
                MESSAGE_VIEW_ACTION_EXPORT_EML.key to true,
                ARCHIVE_MARKS_AS_READ.key to false,
            ),
        )
        val testSubject = createTestSubject(configStore)

        // Act
        testSubject.clearOverride(key = MESSAGE_VIEW_ACTION_EXPORT_EML)

        // Assert
        assertThat(configStore.current.overrides).containsOnly(ARCHIVE_MARKS_AS_READ.key to false)
        assertThat(testSubject.provide(MESSAGE_VIEW_ACTION_EXPORT_EML)).isEqualTo(FeatureFlagResult.Unavailable)
        assertThat(testSubject.provide(ARCHIVE_MARKS_AS_READ)).isEqualTo(FeatureFlagResult.Disabled)
    }

    @Test
    fun `clearAllOverrides should remove every override but keep the targeting key`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            val configStore = FakeFeatureFlagConfigStore(
                targetingKey = TARGETING_UUID,
                overrides = mapOf(
                    MESSAGE_VIEW_ACTION_EXPORT_EML.key to true,
                    ARCHIVE_MARKS_AS_READ.key to false,
                    DISPLAY_IN_APP_NOTIFICATIONS.key to true,
                ),
            )
            val testSubject = createTestSubject(configStore)

            // Act
            testSubject.clearAllOverrides()

            // Assert
            testSubject.overrides.test {
                val overrides = awaitItem()
                assertThat(configStore.current.overrides).isEmpty()
                assertThat(configStore.current.targetingKey).isEqualTo(TARGETING_UUID)
                assertThat(overrides).isEmpty()
                assertThat(testSubject.provide(MESSAGE_VIEW_ACTION_EXPORT_EML)).isEqualTo(FeatureFlagResult.Unavailable)
            }
        }

    @Test
    fun `overrides should be restored from the config store when the provider is recreated`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            val configStore = FakeFeatureFlagConfigStore()
            createTestSubject(configStore).setOverride(key = MESSAGE_VIEW_ACTION_EXPORT_EML, enabled = true)

            // Act
            val recreatedTestSubject = createTestSubject(configStore)

            // Assert
            recreatedTestSubject.overrides.test {
                val overrides = awaitItem()
                assertThat(overrides).containsOnly(MESSAGE_VIEW_ACTION_EXPORT_EML.key to true)
                assertThat(recreatedTestSubject.provide(MESSAGE_VIEW_ACTION_EXPORT_EML))
                    .isEqualTo(FeatureFlagResult.Enabled)
            }
        }

    @Test
    fun `data should emit the overrides after each mutation`() = runTest(UnconfinedTestDispatcher()) {
        // Arrange
        val testSubject = createTestSubject(FakeFeatureFlagConfigStore())
        val emissions = mutableListOf<FlagOverrides>()
        testSubject.data
            .onEach { data -> emissions += data.overrides }
            .launchIn(backgroundScope)

        // Act
        testSubject.setOverride(key = MESSAGE_VIEW_ACTION_EXPORT_EML, enabled = true)
        testSubject.setOverride(key = ARCHIVE_MARKS_AS_READ, enabled = false)
        testSubject.clearOverride(key = MESSAGE_VIEW_ACTION_EXPORT_EML)
        testSubject.clearAllOverrides()

        // Assert
        assertThat(emissions).containsExactly(
            emptyMap<String, Boolean>(),
            mapOf(MESSAGE_VIEW_ACTION_EXPORT_EML.key to true),
            mapOf(MESSAGE_VIEW_ACTION_EXPORT_EML.key to true, ARCHIVE_MARKS_AS_READ.key to false),
            mapOf(ARCHIVE_MARKS_AS_READ.key to false),
            emptyMap<String, Boolean>(),
        )
    }

    private suspend fun TestScope.createTestSubject(
        configStore: FeatureFlagConfigStore,
    ): RuntimeDebugOverrideFeatureFlagProvider = RuntimeDebugOverrideFeatureFlagProvider(
        configStore = configStore,
        logger = TestLogger(),
        scope = backgroundScope,
    ).also { provider ->
        provider.initialize(initialContext = context())
    }

    private companion object {
        const val TARGETING_KEY = "targeting-key"
        val TARGETING_UUID: Uuid = Uuid.parse("f2d9a9a0-6d3f-4b5e-9f4a-1d2c3b4a5e6f")

        fun context(): FeatureFlagContext = ImmutableFeatureFlagContext(targetingKey = TARGETING_KEY)
    }
}

@OptIn(ExperimentalUuidApi::class)
private class FakeFeatureFlagConfigStore(
    targetingKey: Uuid? = null,
    overrides: FlagOverrides = emptyMap(),
) : FeatureFlagConfigStore {
    private val state = MutableStateFlow(
        FeatureFlagConfigData(targetingKey = targetingKey, overrides = overrides),
    )

    override val config: Flow<FeatureFlagConfigData> = state

    /** The currently persisted configuration, so tests can assert what was written to the store. */
    val current: FeatureFlagConfigData get() = state.value

    override suspend fun update(transform: (FeatureFlagConfigData?) -> FeatureFlagConfigData) {
        state.update { current -> transform(current) }
    }

    override suspend fun clear() {
        state.value = FeatureFlagConfigData.DEFAULT
    }
}
