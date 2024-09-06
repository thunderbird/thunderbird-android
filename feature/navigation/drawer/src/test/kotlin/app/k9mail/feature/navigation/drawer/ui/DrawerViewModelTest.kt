package app.k9mail.feature.navigation.drawer.ui

import app.k9mail.core.ui.compose.testing.MainDispatcherRule
import app.k9mail.core.ui.compose.testing.mvi.eventStateTest
import app.k9mail.feature.navigation.drawer.ui.DrawerContract.Event
import app.k9mail.feature.navigation.drawer.ui.DrawerContract.State
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule

@OptIn(ExperimentalCoroutinesApi::class)
class DrawerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val testSubject = DrawerViewModel()

    @Test
    fun `should change loading state when OnRefresh event is received`() = runTest {
        eventStateTest(
            viewModel = testSubject,
            initialState = State(isLoading = false),
            event = Event.OnRefresh,
            expectedState = State(isLoading = true),
            coroutineScope = backgroundScope,
        )

        advanceUntilIdle()

        assertThat(testSubject.state.value.isLoading).isEqualTo(false)
    }
}
