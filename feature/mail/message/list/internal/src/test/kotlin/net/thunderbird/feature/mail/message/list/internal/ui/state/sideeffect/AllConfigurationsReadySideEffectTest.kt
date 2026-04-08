package net.thunderbird.feature.mail.message.list.internal.ui.state.sideeffect

import androidx.compose.ui.graphics.Color
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import dev.mokkery.spy
import dev.mokkery.verifySuspend
import kotlin.test.Test
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.common.action.SwipeAction
import net.thunderbird.core.common.action.SwipeActions
import net.thunderbird.core.common.state.sideeffect.StateSideEffectHandler
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.core.preference.display.visualSettings.message.list.MessageListDateTimeFormat
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.UnifiedAccountId
import net.thunderbird.feature.mail.folder.api.FolderType
import net.thunderbird.feature.mail.message.list.domain.model.SortCriteria
import net.thunderbird.feature.mail.message.list.domain.model.SortType
import net.thunderbird.feature.mail.message.list.preferences.MessageListPreferences
import net.thunderbird.feature.mail.message.list.ui.event.MessageListEvent
import net.thunderbird.feature.mail.message.list.ui.state.Account
import net.thunderbird.feature.mail.message.list.ui.state.Folder
import net.thunderbird.feature.mail.message.list.ui.state.MessageListMetadata
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState

class AllConfigurationsReadySideEffectTest {

    @Test
    fun `handle() should return Consumed when state is WarmingUp and isReady is true`() = runTest {
        // Arrange
        val testSubject = createTestSubject()

        // Act
        val result = testSubject.handle(
            event = MessageListEvent.SwipeActionsLoaded(persistentMapOf()),
            oldState = MessageListState.WarmingUp(),
            newState = createReadyWarmingUpState(),
        )

        // Assert
        assertThat(result).isEqualTo(StateSideEffectHandler.ConsumeResult.Consumed)
    }

    @Test
    fun `handle() should return Ignored when state is WarmingUp but metadata is not ready`() = runTest {
        // Arrange
        val testSubject = createTestSubject()

        // Act
        val result = testSubject.handle(
            event = MessageListEvent.LoadConfigurations,
            oldState = MessageListState.WarmingUp(),
            newState = MessageListState.WarmingUp(),
        )

        // Assert
        assertThat(result).isEqualTo(StateSideEffectHandler.ConsumeResult.Ignored)
    }

    @Test
    fun `handle() should return Ignored when state is WarmingUp but preferences is null`() = runTest {
        // Arrange
        val testSubject = createTestSubject()
        val state = createReadyWarmingUpState().copy(preferences = null)

        // Act
        val result = testSubject.handle(
            event = MessageListEvent.UpdatePreferences(createPreferences()),
            oldState = MessageListState.WarmingUp(),
            newState = state,
        )

        // Assert
        assertThat(result).isEqualTo(StateSideEffectHandler.ConsumeResult.Ignored)
    }

    @Test
    fun `handle() should return Ignored when state is not WarmingUp`() = runTest {
        // Arrange
        val testSubject = createTestSubject()

        // Act
        val result = testSubject.handle(
            event = MessageListEvent.AllConfigsReady,
            oldState = MessageListState.WarmingUp(),
            newState = MessageListState.LoadedMessages(
                metadata = createReadyMetadata(),
                preferences = createPreferences(),
                messages = kotlinx.collections.immutable.persistentListOf(),
            ),
        )

        // Assert
        assertThat(result).isEqualTo(StateSideEffectHandler.ConsumeResult.Ignored)
    }

    @Test
    fun `handle() should dispatch AllConfigsReady event`() = runTest {
        // Arrange
        val dispatch = spy<suspend (MessageListEvent) -> Unit>(obj = {})
        val testSubject = createTestSubject(dispatch = dispatch)
        val state = createReadyWarmingUpState()

        // Act
        testSubject.handle(
            event = MessageListEvent.SwipeActionsLoaded(persistentMapOf()),
            oldState = MessageListState.WarmingUp(),
            newState = state,
        )

        // Assert
        verifySuspend { dispatch(MessageListEvent.AllConfigsReady) }
    }

    @Test
    fun `factory should create AllConfigurationsReadySideEffect`() = runTest {
        // Arrange
        val factory = AllConfigurationsReadySideEffect.Factory(
            logger = TestLogger(),
        )

        // Act
        val result = factory.create(
            scope = this,
            dispatch = {},
            dispatchUiEffect = {},
        )

        // Assert
        assertThat(result).isInstanceOf(AllConfigurationsReadySideEffect::class)
    }

    private fun createTestSubject(
        dispatch: suspend (MessageListEvent) -> Unit = {},
    ) = AllConfigurationsReadySideEffect(
        logger = TestLogger(),
        dispatch = dispatch,
    )

    private fun createReadyWarmingUpState() = MessageListState.WarmingUp(
        metadata = createReadyMetadata(),
        preferences = createPreferences(),
    )

    private fun createReadyMetadata() = MessageListMetadata(
        folder = Folder(
            id = "fake",
            account = Account(id = UnifiedAccountId, color = Color.Unspecified),
            name = "Inbox",
            type = FolderType.INBOX,
        ),
        swipeActions = persistentMapOf(
            AccountIdFactory.create() to SwipeActions(SwipeAction.None, SwipeAction.None),
        ),
        sortCriteriaPerAccount = persistentMapOf(null to SortCriteria(primary = SortType.DateDesc)),
        activeMessage = null,
        isActive = false,
    )

    private fun createPreferences() = MessageListPreferences(
        density = net.thunderbird.core.preference.display.visualSettings.message.list.UiDensity.Default,
        groupConversations = false,
        showCorrespondentNames = false,
        showMessageAvatar = false,
        showFavouriteButton = false,
        senderAboveSubject = false,
        excerptLines = 1,
        dateTimeFormat = MessageListDateTimeFormat.Contextual,
        actionRequiringUserConfirmation = persistentSetOf(),
        colorizeBackgroundWhenRead = false,
    )
}
