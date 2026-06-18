package net.thunderbird.core.ui.testing

import assertk.assertThat
import assertk.assertions.isSameInstanceAs
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler

private val mainDispatcher = StandardTestDispatcher(TestCoroutineScheduler())
private val explicitEffectDispatcher = StandardTestDispatcher(TestCoroutineScheduler())

@OptIn(ExperimentalCoroutinesApi::class)
class ValidateComposeUiTestHarnessMainDispatcher {

    @Test
    fun `resolveEffectContext should use main dispatcher for default effect context`() {
        val result = resolveEffectContext(
            effectContext = EmptyCoroutineContext,
            mainDispatcher = mainDispatcher,
        )

        assertThat(result).isSameInstanceAs(mainDispatcher)
    }

    @Test
    fun `resolveEffectContext should preserve default effect context when main dispatcher is absent`() {
        val result = resolveEffectContext(
            effectContext = EmptyCoroutineContext,
            mainDispatcher = null,
        )

        assertThat(result).isSameInstanceAs(EmptyCoroutineContext)
    }

    @Test
    fun `resolveEffectContext should use explicit effect context over main dispatcher`() {
        val result = resolveEffectContext(
            effectContext = explicitEffectDispatcher,
            mainDispatcher = mainDispatcher,
        )

        assertThat(result).isSameInstanceAs(explicitEffectDispatcher)
    }
}
