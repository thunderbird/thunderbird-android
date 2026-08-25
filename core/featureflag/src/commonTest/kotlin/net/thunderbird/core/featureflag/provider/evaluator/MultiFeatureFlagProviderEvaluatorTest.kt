package net.thunderbird.core.featureflag.provider.evaluator

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import net.thunderbird.core.featureflag.FeatureFlagKey
import net.thunderbird.core.featureflag.FeatureFlagResult
import net.thunderbird.core.featureflag.keys.GeneratedFeatureFlagKey.MESSAGE_VIEW_ACTION_EXPORT_EML
import net.thunderbird.core.featureflag.provider.CatalogFeatureFlagProvider
import net.thunderbird.core.featureflag.provider.CatalogFeatureFlagProvider.State
import net.thunderbird.core.featureflag.provider.ProviderMetadata
import net.thunderbird.core.logging.testing.TestLogger

class MultiFeatureFlagProviderEvaluatorTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @Test
    fun `provide should return Enabled when the first provider returns Enabled`() {
        // Arrange
        val recorder = InvocationRecorder()
        val testSubject = createTestSubject(
            recorder.provider(name = FIRST, result = FeatureFlagResult.Enabled),
            recorder.provider(name = SECOND, result = FeatureFlagResult.Disabled),
        )

        // Act
        val result = testSubject.provide(MESSAGE_VIEW_ACTION_EXPORT_EML)

        // Assert
        assertThat(result).isEqualTo(FeatureFlagResult.Enabled)
    }

    @Test
    fun `provide should return Disabled when the first provider returns Disabled`() {
        // Arrange
        val recorder = InvocationRecorder()
        val testSubject = createTestSubject(
            recorder.provider(name = FIRST, result = FeatureFlagResult.Disabled),
            recorder.provider(name = SECOND, result = FeatureFlagResult.Enabled),
        )

        // Act
        val result = testSubject.provide(MESSAGE_VIEW_ACTION_EXPORT_EML)

        // Assert
        assertThat(result).isEqualTo(FeatureFlagResult.Disabled)
    }

    @Test
    fun `provide should return Enabled when the first provider is Unavailable and the second returns Enabled`() {
        // Arrange
        val recorder = InvocationRecorder()
        val testSubject = createTestSubject(
            recorder.provider(name = FIRST, result = FeatureFlagResult.Unavailable),
            recorder.provider(name = SECOND, result = FeatureFlagResult.Enabled),
        )

        // Act
        val result = testSubject.provide(MESSAGE_VIEW_ACTION_EXPORT_EML)

        // Assert
        assertThat(result).isEqualTo(FeatureFlagResult.Enabled)
        assertThat(recorder.invocations).containsExactly(FIRST, SECOND)
    }

    @Test
    fun `provide should return Disabled when the first provider is Unavailable and the second returns Disabled`() {
        // Arrange
        val recorder = InvocationRecorder()
        val testSubject = createTestSubject(
            recorder.provider(name = FIRST, result = FeatureFlagResult.Unavailable),
            recorder.provider(name = SECOND, result = FeatureFlagResult.Disabled),
        )

        // Act
        val result = testSubject.provide(MESSAGE_VIEW_ACTION_EXPORT_EML)

        // Assert
        assertThat(result).isEqualTo(FeatureFlagResult.Disabled)
        assertThat(recorder.invocations).containsExactly(FIRST, SECOND)
    }

    @Test
    fun `provide should return Unavailable when every provider returns Unavailable`() {
        // Arrange
        val recorder = InvocationRecorder()
        val testSubject = createTestSubject(
            recorder.provider(name = FIRST, result = FeatureFlagResult.Unavailable),
            recorder.provider(name = SECOND, result = FeatureFlagResult.Unavailable),
            recorder.provider(name = THIRD, result = FeatureFlagResult.Unavailable),
        )

        // Act
        val result = testSubject.provide(MESSAGE_VIEW_ACTION_EXPORT_EML)

        // Assert
        assertThat(result).isEqualTo(FeatureFlagResult.Unavailable)
    }

    @Test
    fun `provide should query the providers in their configured order`() {
        // Arrange
        val recorder = InvocationRecorder()
        val testSubject = createTestSubject(
            recorder.provider(name = FIRST, result = FeatureFlagResult.Unavailable),
            recorder.provider(name = SECOND, result = FeatureFlagResult.Unavailable),
            recorder.provider(name = THIRD, result = FeatureFlagResult.Unavailable),
        )

        // Act
        testSubject.provide(MESSAGE_VIEW_ACTION_EXPORT_EML)

        // Assert
        assertThat(recorder.invocations).containsExactly(FIRST, SECOND, THIRD)
    }

    @Test
    fun `provide should not query the providers after a concrete result`() {
        // Arrange
        val recorder = InvocationRecorder()
        val notQueriedProvider = recorder.provider(name = THIRD, result = FeatureFlagResult.Enabled)
        val testSubject = createTestSubject(
            recorder.provider(name = FIRST, result = FeatureFlagResult.Unavailable),
            recorder.provider(name = SECOND, result = FeatureFlagResult.Disabled),
            notQueriedProvider,
        )

        // Act
        testSubject.provide(MESSAGE_VIEW_ACTION_EXPORT_EML)

        // Assert
        assertThat(recorder.invocations).containsExactly(FIRST, SECOND)
        assertThat(notQueriedProvider.provideCount).isEqualTo(0)
    }

    private fun createTestSubject(
        vararg providers: CatalogFeatureFlagProvider,
    ): MultiFeatureFlagProviderEvaluator = DefaultMultiFeatureFlagProviderEvaluator(
        providers = providers.toList(),
        logger = TestLogger(),
    )

    private companion object {
        const val FIRST = "first"
        const val SECOND = "second"
        const val THIRD = "third"
    }
}

/**
 * Records the order in which the providers it creates are queried, so tests can assert both the
 * first-match strategy and that later providers are left untouched.
 */
private class InvocationRecorder {
    val invocations: MutableList<String> = mutableListOf()

    fun provider(name: String, result: FeatureFlagResult): FakeCatalogFeatureFlagProvider =
        FakeCatalogFeatureFlagProvider(name = name, result = result, invocations = invocations)
}

private class FakeCatalogFeatureFlagProvider(
    private val name: String,
    private val result: FeatureFlagResult,
    private val invocations: MutableList<String>,
) : CatalogFeatureFlagProvider {
    var provideCount: Int = 0
        private set

    override val state: StateFlow<State> = MutableStateFlow(State.Resolved)

    override val metadata: ProviderMetadata = FakeProviderMetadata(name)

    override fun provide(key: FeatureFlagKey): FeatureFlagResult {
        provideCount++
        invocations += name
        return result
    }

    override fun toString(): String = "fake feature-flag provider '$name'"
}

private data class FakeProviderMetadata(override val name: String) : ProviderMetadata
