package net.thunderbird.feature.mail.message.list.internal.ui.state.sideeffect

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.spy
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import kotlin.random.Random
import kotlin.test.Test
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.core.preference.display.visualSettings.message.list.UiDensity
import net.thunderbird.feature.mail.message.list.domain.DomainContract.UseCase.GetMessageListPreferences
import net.thunderbird.feature.mail.message.list.preferences.ActionRequiringUserConfirmation
import net.thunderbird.feature.mail.message.list.preferences.MessageListDateTimeFormat
import net.thunderbird.feature.mail.message.list.preferences.MessageListPreferences
import net.thunderbird.feature.mail.message.list.ui.event.MessageListEvent
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState

class LoadPreferencesSideEffectTest {
    @Test
    fun `accept() should return true if event is LoadConfigurations`() = runTest {
        // Arrange
        val testSubject = LoadPreferencesSideEffect(
            dispatch = {},
            scope = backgroundScope,
            logger = TestLogger(),
            getMessageListPreferences = mock(),
        )

        // Act
        val actual = testSubject.accept(
            event = MessageListEvent.LoadConfigurations,
            newState = MessageListState.WarmingUp(),
        )

        // Assert
        assertThat(actual).isTrue()
    }

    @Test
    fun `accept() should return false if event is not LoadConfigurations`() = runTest {
        // Arrange
        val testSubject = LoadPreferencesSideEffect(
            dispatch = {},
            scope = backgroundScope,
            logger = TestLogger(),
            getMessageListPreferences = mock(),
        )

        // Act
        val actual = testSubject.accept(
            event = MessageListEvent.ExitSelectionMode,
            newState = MessageListState.WarmingUp(),
        )

        // Assert
        assertThat(actual).isFalse()
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `handle() should start getMessageListPreferences flow and dispatch UpdatePreferences event`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            val dispatch = spy<(MessageListEvent) -> Unit>(obj = {})
            val initialPreferences = createMessageListPreferences(density = UiDensity.Compact)
            val fakeGetMessageListPreferences = FakeGetMessageListPreferences(initialPreferences)
            val testSubject = LoadPreferencesSideEffect(
                dispatch = dispatch,
                scope = backgroundScope,
                logger = TestLogger(),
                getMessageListPreferences = fakeGetMessageListPreferences,
            )
            val oldState = MessageListState.WarmingUp()
            val newState = oldState.withMetadata { copy(isActive = true) }

            // Act
            testSubject.handle(oldState, newState)

            // Assert
            verify(mode = VerifyMode.exactly(1)) {
                dispatch(MessageListEvent.UpdatePreferences(initialPreferences))
            }
        }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `getMessageListPreferences() should dispatch UpdatePreferences event whenever a new preferences is emitted`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            val dispatch = spy<(MessageListEvent) -> Unit>(obj = {})
            val initialPreferences = createMessageListPreferences(density = UiDensity.Compact)
            val fakeGetMessageListPreferences = FakeGetMessageListPreferences(initialPreferences)
            val testSubject = LoadPreferencesSideEffect(
                dispatch = dispatch,
                scope = backgroundScope,
                logger = TestLogger(),
                getMessageListPreferences = fakeGetMessageListPreferences,
            )
            val oldState = MessageListState.WarmingUp()
            val newState = oldState.withMetadata { copy(isActive = true) }

            // Act
            testSubject.handle(oldState, newState)
            repeat(times = 10) {
                fakeGetMessageListPreferences.emit(
                    preferences = createMessageListPreferences(
                        density = UiDensity.entries.random(),
                        showMessageAvatar = Random.nextBoolean(),
                        showFavouriteButton = Random.nextBoolean(),
                        senderAboveSubject = Random.nextBoolean(),
                        colorizeBackgroundWhenRead = Random.nextBoolean(),
                    ),
                )
            }

            // Assert
            verify(mode = VerifyMode.exactly(11)) {
                dispatch(any())
            }
        }

    private fun createMessageListPreferences(
        density: UiDensity = UiDensity.Default,
        groupConversations: Boolean = false,
        showCorrespondentNames: Boolean = false,
        showMessageAvatar: Boolean = false,
        showFavouriteButton: Boolean = false,
        senderAboveSubject: Boolean = false,
        excerptLines: Int = 1,
        dateTimeFormat: MessageListDateTimeFormat = MessageListDateTimeFormat.Auto,
        useVolumeKeyNavigation: Boolean = false,
        serverSearchLimit: Int = 0,
        actionRequiringUserConfirmation: ImmutableSet<ActionRequiringUserConfirmation> = persistentSetOf(),
        colorizeBackgroundWhenRead: Boolean = false,
    ) = MessageListPreferences(
        density = density,
        groupConversations = groupConversations,
        showCorrespondentNames = showCorrespondentNames,
        showMessageAvatar = showMessageAvatar,
        showFavouriteButton = showFavouriteButton,
        senderAboveSubject = senderAboveSubject,
        excerptLines = excerptLines,
        dateTimeFormat = dateTimeFormat,
        useVolumeKeyNavigation = useVolumeKeyNavigation,
        serverSearchLimit = serverSearchLimit,
        actionRequiringUserConfirmation = actionRequiringUserConfirmation,
        colorizeBackgroundWhenRead = colorizeBackgroundWhenRead,
    )

    private inner class FakeGetMessageListPreferences(
        initialValue: MessageListPreferences = createMessageListPreferences(),
    ) : GetMessageListPreferences {
        private val preferences = MutableStateFlow(initialValue)

        override fun invoke(): Flow<MessageListPreferences> = preferences

        suspend fun emit(preferences: MessageListPreferences) = this.preferences.emit(preferences)
    }
}
