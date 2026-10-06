package net.thunderbird.core.featureflag.ui

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import net.thunderbird.components.core.outcome.Outcome
import net.thunderbird.components.ui.testing.coroutines.MainDispatcherHelper
import net.thunderbird.core.featureflag.domain.RemoteFeatureFlagDomainContract.FetchEnabledFeatureFlags
import net.thunderbird.core.featureflag.domain.RemoteFeatureFlagDomainContract.UpdateRemoteFeatureFlagAvailability
import net.thunderbird.core.featureflag.keys.GeneratedFeatureFlagKey.ARCHIVE_MARKS_AS_READ

class RemoteFeatureFlagViewModelTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    val mainDispatcherHelper = MainDispatcherHelper(UnconfinedTestDispatcher())

    @BeforeTest
    fun setUp() {
        mainDispatcherHelper.setUp()
    }

    @AfterTest
    fun tearDown() {
        mainDispatcherHelper.tearDown()
    }

    @Test
    fun `init should map the enabled feature flags to the state`() = runTest {
        // Arrange
        val fetchEnabledFeatureFlags = FetchEnabledFeatureFlags {
            flowOf(fetchSuccess(enabled = true))
        }

        // Act
        val testSubject = createTestSubject(fetchEnabledFeatureFlags = fetchEnabledFeatureFlags)

        // Assert
        assertThat(testSubject.state.value).isEqualTo(
            RemoteFeatureFlagUiContract.State(
                remoteFeatureFlagEnabled = true,
                remoteFeatureFlagCatalogAvailable = true,
                location = FeatureFlagLocation.Remote,
                flags = persistentListOf(ARCHIVE_MARKS_AS_READ),
            ),
        )
    }

    @Test
    fun `init should map the remote catalog as unavailable to the state when no catalog is loaded`() = runTest {
        // Arrange
        val fetchEnabledFeatureFlags = FetchEnabledFeatureFlags {
            flowOf(fetchSuccess(enabled = false, available = false))
        }

        // Act
        val testSubject = createTestSubject(fetchEnabledFeatureFlags = fetchEnabledFeatureFlags)

        // Assert
        assertThat(testSubject.state.value).isEqualTo(
            RemoteFeatureFlagUiContract.State(
                remoteFeatureFlagEnabled = false,
                remoteFeatureFlagCatalogAvailable = false,
                location = FeatureFlagLocation.Local,
                flags = persistentListOf(ARCHIVE_MARKS_AS_READ),
            ),
        )
    }

    @Test
    fun `init should emit ShowError with LoadFailed when the feature flag config cannot be read`() = runTest {
        // Arrange
        val fetchResults =
            MutableSharedFlow<Outcome<FetchEnabledFeatureFlags.Success, FetchEnabledFeatureFlags.Failure>>()
        val testSubject = createTestSubject(fetchEnabledFeatureFlags = { fetchResults })

        testSubject.effect.test {
            // Act
            fetchResults.emit(
                Outcome.failure(FetchEnabledFeatureFlags.Failure.ConfigReadFailed(cause = IOException())),
            )

            // Assert
            assertThat(awaitItem()).isEqualTo(
                RemoteFeatureFlagUiContract.Effect.ShowError(RemoteFeatureFlagError.LoadFailed),
            )
        }
        assertThat(testSubject.state.value).isEqualTo(RemoteFeatureFlagUiContract.State())
    }

    @Test
    fun `OnEnableChange should emit ShowError with UpdateFailed and keep the previous value when the update fails`() =
        runTest {
            // Arrange
            val testSubject = createTestSubject(
                fetchEnabledFeatureFlags = { flowOf(fetchSuccess(enabled = true)) },
                updateRemoteFeatureFlagAvailability = {
                    Outcome.failure(
                        UpdateRemoteFeatureFlagAvailability.Failure.ConfigUpdateFailed(cause = IOException()),
                    )
                },
            )

            testSubject.effect.test {
                // Act
                testSubject.event(RemoteFeatureFlagUiContract.Event.OnEnableChange(enabled = false))

                // Assert
                assertThat(awaitItem()).isEqualTo(
                    RemoteFeatureFlagUiContract.Effect.ShowError(RemoteFeatureFlagError.UpdateFailed),
                )
            }
            assertThat(testSubject.state.value.remoteFeatureFlagEnabled).isEqualTo(true)
        }

    @Test
    fun `OnEnableChange should update the value without emitting an effect when the update succeeds`() = runTest {
        // Arrange
        val testSubject = createTestSubject(
            fetchEnabledFeatureFlags = { flowOf(fetchSuccess(enabled = true)) },
            updateRemoteFeatureFlagAvailability = { Outcome.success() },
        )

        testSubject.effect.test {
            // Act
            testSubject.event(RemoteFeatureFlagUiContract.Event.OnEnableChange(enabled = false))

            // Assert
            expectNoEvents()
        }
        assertThat(testSubject.state.value.remoteFeatureFlagEnabled).isEqualTo(false)
    }

    private fun createTestSubject(
        fetchEnabledFeatureFlags: FetchEnabledFeatureFlags = FetchEnabledFeatureFlags { MutableSharedFlow() },
        updateRemoteFeatureFlagAvailability: UpdateRemoteFeatureFlagAvailability =
            UpdateRemoteFeatureFlagAvailability { Outcome.success() },
    ): RemoteFeatureFlagViewModel = RemoteFeatureFlagViewModel(
        fetchEnabledFeatureFlags = fetchEnabledFeatureFlags,
        updateRemoteFeatureFlagAvailability = updateRemoteFeatureFlagAvailability,
    )

    private fun fetchSuccess(enabled: Boolean, available: Boolean = true) = Outcome.success(
        FetchEnabledFeatureFlags.Success(
            enabled = enabled,
            available = available,
            isRuntimeOverride = false,
            flags = listOf(ARCHIVE_MARKS_AS_READ),
        ),
    )
}
