package net.thunderbird.feature.mail.message.list.internal.ui.state.machine

import androidx.compose.ui.graphics.Color
import app.cash.turbine.test
import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import assertk.assertions.isTrue
import assertk.assertions.prop
import dev.mokkery.spy
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import kotlin.test.Test
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.common.action.SwipeAction
import net.thunderbird.core.common.action.SwipeActions
import net.thunderbird.core.preference.display.visualSettings.message.list.UiDensity
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.mail.message.list.preferences.ActionRequiringUserConfirmation
import net.thunderbird.feature.mail.message.list.preferences.MessageListDateTimeFormat
import net.thunderbird.feature.mail.message.list.preferences.MessageListPreferences
import net.thunderbird.feature.mail.message.list.ui.event.MessageItemEvent
import net.thunderbird.feature.mail.message.list.ui.event.MessageListEvent
import net.thunderbird.feature.mail.message.list.ui.event.MessageListSearchEvent
import net.thunderbird.feature.mail.message.list.ui.state.Account
import net.thunderbird.feature.mail.message.list.ui.state.EmailIdentity
import net.thunderbird.feature.mail.message.list.ui.state.MessageItemAttachment
import net.thunderbird.feature.mail.message.list.ui.state.MessageItemUi
import net.thunderbird.feature.mail.message.list.ui.state.MessageItemUi.State
import net.thunderbird.feature.mail.message.list.ui.state.MessageListMetadata
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState
import net.thunderbird.feature.mail.message.list.ui.state.SortType

@Suppress("MaxLineLength")
@OptIn(ExperimentalCoroutinesApi::class)
class MessageListStateMachineTest {
    private fun TestScope.createStateMachine(dispatch: (MessageListEvent) -> Unit = {}) = MessageListStateMachine(
        scope = this,
        dispatch = dispatch,
    )

    // region [WarmingUp state]
    @Test
    fun `stateMachine should trigger LoadConfigurations event when it is initialized`() = runTest {
        // Arrange
        val dispatch = spy<(MessageListEvent) -> Unit>(obj = {})
        // Act
        createStateMachine(dispatch)
        advanceUntilIdle()
        // Assert
        verify(mode = VerifyMode.exactly(1)) { dispatch(MessageListEvent.LoadConfigurations) }
    }

    @Test
    fun `process() should stay on WarmingUp state when state is WarmingUp and event is LoadConfigurations`() =
        runTest {
            // Arrange
            val stateMachine = createStateMachine()
            advanceUntilIdle()

            // Act
            stateMachine.process(event = MessageListEvent.LoadConfigurations)

            // Assert
            stateMachine.currentState.test {
                val state = awaitItem()
                expectNoEvents()
                assertThat(state).isInstanceOf<MessageListState.WarmingUp>()
            }
        }

    @Test
    fun `process() should stay on WarmingUp state when state is WarmingUp and event is UpdatePreferences`() =
        runTest {
            // Arrange
            val stateMachine = createStateMachine()
            advanceUntilIdle()

            // Act
            stateMachine.process(
                event = MessageListEvent.UpdatePreferences(
                    preferences = createMessageListPreferences(),
                ),
            )

            // Assert
            stateMachine.currentState.test {
                val state = awaitItem()
                expectNoEvents()
                assertThat(state).isInstanceOf<MessageListState.WarmingUp>()
            }
        }

    @Test
    fun `process() should stay on WarmingUp state when state is WarmingUp and event is SortTypesLoaded`() =
        runTest {
            // Arrange
            val stateMachine = createStateMachine()
            advanceUntilIdle()

            // Act
            stateMachine.process(event = MessageListEvent.SortTypesLoaded(emptyMap()))

            // Assert
            stateMachine.currentState.test {
                val state = awaitItem()
                expectNoEvents()
                assertThat(state).isInstanceOf<MessageListState.WarmingUp>()
            }
        }

    @Test
    fun `process() should not change state to LoadedMessages when event is AllConfigsReady but state is not ready`() =
        runTest {
            // Arrange
            val stateMachine = createStateMachine()
            advanceUntilIdle()

            // Act
            stateMachine.process(event = MessageListEvent.AllConfigsReady)

            // Assert
            stateMachine.currentState.test {
                val state = awaitItem()
                expectNoEvents()
                assertThat(state).isInstanceOf<MessageListState.WarmingUp>()
            }
        }

    @Test
    fun `process() should change state to LoadedMessages when event is AllConfigsReady`() = runTest {
        // Arrange
        val stateMachine = createStateMachine()
        val preferences = createMessageListPreferences()
        val sortTypes = mapOf<AccountId?, SortType>(null to SortType.DateDesc)
        val swipeActions = mapOf<AccountId, SwipeActions>(
            AccountIdFactory.create() to SwipeActions(SwipeAction.None, SwipeAction.None),
        )
        advanceUntilIdle()

        // Act
        stateMachine.process(event = MessageListEvent.AllConfigsReady)

        // Assert
        stateMachine.currentState.test {
            assertThat(awaitItem()).isInstanceOf<MessageListState.WarmingUp>()

            stateMachine.process(MessageListEvent.UpdatePreferences(preferences))
            assertThat(awaitItem()).isInstanceOf<MessageListState.WarmingUp>()

            stateMachine.process(MessageListEvent.SortTypesLoaded(sortTypes))
            assertThat(awaitItem()).isInstanceOf<MessageListState.WarmingUp>()

            stateMachine.process(MessageListEvent.SwipeActionsLoaded(swipeActions))
            assertThat(awaitItem()).isInstanceOf<MessageListState.WarmingUp>()
            stateMachine.process(event = MessageListEvent.AllConfigsReady)
            assertThat(awaitItem())
                .isInstanceOf<MessageListState.LoadingMessages>()
                .all {
                    prop(MessageListState.LoadingMessages::preferences).isEqualTo(preferences)
                    prop(MessageListState.LoadingMessages::progress).isEqualTo(0f)
                    transform { it.metadata }.all {
                        prop(MessageListMetadata::swipeActions).isEqualTo(swipeActions)
                        prop(MessageListMetadata::selectedSortTypes).isEqualTo(sortTypes)
                        prop(MessageListMetadata::folder).isNull()
                    }
                }

            expectNoEvents()
        }
    }
    // endregion [WarmingUp state]

    // region [LoadingMessages state]
    @Test
    fun `process() should update LoadingMessages state with progress when state is LoadingMessages and event is UpdateLoadingProgress`() =
        runTest {
            // Arrange
            val firstExpectedProgress = .3f
            val secondExpectedProgress = .5f
            val lastExpectedProgress = 1f
            val stateMachine = createStateMachineOnLoadingState()
            advanceUntilIdle()

            stateMachine.currentState.test {
                // enforce correct state before acting.
                assertThat(expectMostRecentItem()).isInstanceOf<MessageListState.LoadingMessages>()

                // Act (Phase 1)
                stateMachine.process(MessageListEvent.UpdateLoadingProgress(progress = firstExpectedProgress))

                // Assert (Phase 1)
                assertThat(awaitItem())
                    .isInstanceOf<MessageListState.LoadingMessages>()
                    .prop(MessageListState.LoadingMessages::progress)
                    .isEqualTo(firstExpectedProgress)

                // Act (Phase 2)
                stateMachine.process(MessageListEvent.UpdateLoadingProgress(progress = secondExpectedProgress))

                // Assert (Phase 2)
                assertThat(awaitItem())
                    .isInstanceOf<MessageListState.LoadingMessages>()
                    .prop(MessageListState.LoadingMessages::progress)
                    .isEqualTo(secondExpectedProgress)

                // Act (Phase 3)
                stateMachine.process(MessageListEvent.UpdateLoadingProgress(progress = lastExpectedProgress))

                // Assert (Phase 3)
                assertThat(awaitItem())
                    .isInstanceOf<MessageListState.LoadingMessages>()
                    .prop(MessageListState.LoadingMessages::progress)
                    .isEqualTo(lastExpectedProgress)

                expectNoEvents()
            }
        }

    @Test
    fun `process() should not move to LoadedMessages state when state is LoadingMessages, event is MessagesLoaded but progress is not 1f`() =
        runTest {
            // Arrange
            val stateMachine = createStateMachineOnLoadingState()
            advanceUntilIdle()

            stateMachine.currentState.test {
                // enforce correct state before acting.
                assertThat(expectMostRecentItem()).isInstanceOf<MessageListState.LoadingMessages>()

                // Act (Phase 1)
                stateMachine.process(MessageListEvent.UpdateLoadingProgress(progress = .5f))

                // Assert (Phase 1)
                assertThat(awaitItem())
                    .isInstanceOf<MessageListState.LoadingMessages>()

                // Act (Phase 2)
                stateMachine.process(MessageListEvent.MessagesLoaded(messages = emptyList()))

                // Assert (Phase 2)
                expectNoEvents()
            }
        }

    @Test
    fun `process() should move to LoadedMessages state when state is LoadingMessages, progress is 1f, and event is MessagesLoaded`() =
        runTest {
            // Arrange
            val accountId = AccountIdFactory.create()
            val messages = createMessageUiItemList(size = 10, accountId = accountId)
            val preferences: MessageListPreferences = createMessageListPreferences(
                density = UiDensity.Compact,
            )
            val sortTypes: Map<AccountId?, SortType> = mapOf(accountId to SortType.DateDesc)
            val swipeActions: Map<AccountId, SwipeActions> = mapOf(
                accountId to SwipeActions(SwipeAction.None, SwipeAction.None),
            )
            val stateMachine = createStateMachineOnLoadingState(
                preferences = preferences,
                sortTypes = sortTypes,
                swipeActions = swipeActions,
            )
            advanceUntilIdle()

            stateMachine.currentState.test {
                // enforce correct state before acting.
                assertThat(expectMostRecentItem()).isInstanceOf<MessageListState.LoadingMessages>()

                // Act (Phase 1)
                stateMachine.process(MessageListEvent.UpdateLoadingProgress(progress = 1f))

                // Assert (Phase 1)
                assertThat(awaitItem()).isInstanceOf<MessageListState.LoadingMessages>()

                // Act (Phase 2)
                stateMachine.process(MessageListEvent.MessagesLoaded(messages = messages))

                // Assert (Phase 2)
                assertThat(awaitItem())
                    .isInstanceOf<MessageListState.LoadedMessages>()
                    .all {
                        prop(MessageListState.LoadedMessages::preferences).isEqualTo(preferences)
                        prop(MessageListState.LoadedMessages::messages).isEqualTo(messages)
                        transform { it.metadata }.all {
                            prop(MessageListMetadata::folder).isNull()
                            prop(MessageListMetadata::activeMessage).isNull()
                            prop(MessageListMetadata::swipeActions).isEqualTo(swipeActions)
                            prop(MessageListMetadata::selectedSortTypes).isEqualTo(sortTypes)
                        }
                    }
            }
        }
    // endregion [LoadingMessages state]

    // region [LoadedMessages state]
    @Test
    fun `process() should move to SelectingMessages when event is ToggleSelectMessages`() =
        runTest {
            // Arrange
            val messages = createMessageUiItemList(size = 20)
            val toggleSelection = messages.take(5)
            val stateMachine = createStateMachineOnLoadedState(messages = messages)
            advanceUntilIdle()

            stateMachine.currentState.test {
                // enforce correct state before acting.
                assertThat(expectMostRecentItem()).isInstanceOf<MessageListState.LoadedMessages>()

                // Act
                stateMachine.process(MessageItemEvent.ToggleSelectMessages(messages = toggleSelection))

                // Assert
                assertThat(awaitItem())
                    .isInstanceOf<MessageListState.SelectingMessages>()
                    .transform { it.messages }
                    .isEqualTo(
                        messages.mapIndexed { index, message ->
                            if (index in toggleSelection.indices) {
                                message.copy(selected = !message.selected)
                            } else {
                                message
                            }
                        },
                    )

                expectNoEvents()
            }
        }

    @Test
    fun `process() should move to SelectingMessages when event is EnterSelectionMode`() =
        runTest {
            // Arrange
            val messages = createMessageUiItemList(size = 5)
            val stateMachine = createStateMachineOnLoadedState(messages = messages)
            advanceUntilIdle()

            stateMachine.currentState.test {
                // enforce correct state before acting.
                assertThat(expectMostRecentItem()).isInstanceOf<MessageListState.LoadedMessages>()

                // Act
                stateMachine.process(MessageListEvent.EnterSelectionMode)

                // Assert
                assertThat(awaitItem())
                    .isInstanceOf<MessageListState.SelectingMessages>()
                    .prop(MessageListState.SelectingMessages::messages).isEqualTo(messages)

                expectNoEvents()
            }
        }

    @Test
    fun `process() should move to SearchingMessages when event is EnterSearchMode`() =
        runTest {
            // Arrange
            val messages = createMessageUiItemList(size = 10)
            val stateMachine = createStateMachineOnLoadedState(messages = messages)
            advanceUntilIdle()

            stateMachine.currentState.test {
                // enforce correct state before acting.
                assertThat(expectMostRecentItem()).isInstanceOf<MessageListState.LoadedMessages>()

                // Act
                stateMachine.process(MessageListSearchEvent.EnterSearchMode)

                // Assert
                assertThat(awaitItem())
                    .isInstanceOf<MessageListState.SearchingMessages>().all {
                        prop(MessageListState.SearchingMessages::searchQuery).isEqualTo("")
                        prop(MessageListState.SearchingMessages::isServerSearch).isEqualTo(false)
                        prop(MessageListState.SearchingMessages::messages).isEqualTo(messages)
                    }

                expectNoEvents()
            }
        }
    // endregion [LoadedMessages state]

    // region [SelectingMessages state]
    @Test
    fun `process() should update SelectingMessages's message selection when event is ToggleSelectMessages`() =
        runTest {
            // Arrange
            val messages = createMessageUiItemList(size = 20)
            val toggleSelection = messages.take(5)
            val stateMachine = createStateMachineOnSelectingMessages(messages = messages)
            advanceUntilIdle()

            stateMachine.currentState.test {
                // enforce correct state before acting.
                assertThat(expectMostRecentItem()).isInstanceOf<MessageListState.SelectingMessages>()

                // Act
                stateMachine.process(MessageItemEvent.ToggleSelectMessages(messages = toggleSelection))

                // Assert
                assertThat(awaitItem())
                    .isInstanceOf<MessageListState.SelectingMessages>()
                    .transform { it.messages }
                    .isEqualTo(
                        messages.mapIndexed { index, message ->
                            if (index in toggleSelection.indices) {
                                message.copy(selected = !message.selected)
                            } else {
                                message
                            }
                        },
                    )

                expectNoEvents()
            }
        }

    @Test
    fun `process() should move to LoadedMessages when state is SelectingMessages and event is ExitSelectionMode`() =
        runTest {
            // Arrange
            val messages = createMessageUiItemList(size = 20)
                .mapIndexed { index, message -> if (index < 5) message.copy(selected = true) else message }
            val stateMachine = createStateMachineOnSelectingMessages(messages = messages)
            advanceUntilIdle()

            stateMachine.currentState.test {
                // enforce correct state before acting.
                assertThat(expectMostRecentItem()).isInstanceOf<MessageListState.SelectingMessages>()

                // Act
                stateMachine.process(MessageListEvent.ExitSelectionMode)

                // Assert
                assertThat(awaitItem())
                    .isInstanceOf<MessageListState.LoadedMessages>()
                    .prop(MessageListState.LoadedMessages::messages)
                    .isEqualTo(messages.map { it.copy(selected = false) })

                expectNoEvents()
            }
        }
    // endregion [SelectingMessages state]

    // region [SearchingMessages state]
    @Test
    fun `process() should update SearchingMessages's searchQuery when event is UpdateSearchQuery`() =
        runTest {
            // Arrange
            val firstQuery = "first query"
            val secondQuery = "second query"
            val lastQuery = "last query"
            val messages = createMessageUiItemList(size = 20)
            val stateMachine = createStateMachineOnSearchingMessages(messages = messages)
            advanceUntilIdle()

            stateMachine.currentState.test {
                // enforce correct state before acting.
                assertThat(expectMostRecentItem()).isInstanceOf<MessageListState.SearchingMessages>()

                // Act (Phase 1)
                stateMachine.process(MessageListSearchEvent.UpdateSearchQuery(query = firstQuery))

                // Assert (Phase 1)
                assertThat(awaitItem())
                    .isInstanceOf<MessageListState.SearchingMessages>()
                    .prop(MessageListState.SearchingMessages::searchQuery).isEqualTo(firstQuery)

                // Act (Phase 2)
                stateMachine.process(MessageListSearchEvent.UpdateSearchQuery(query = secondQuery))

                // Assert (Phase 2)
                assertThat(awaitItem())
                    .isInstanceOf<MessageListState.SearchingMessages>()
                    .prop(MessageListState.SearchingMessages::searchQuery).isEqualTo(secondQuery)

                // Act (Phase 3)
                stateMachine.process(MessageListSearchEvent.UpdateSearchQuery(query = lastQuery))

                // Assert (Phase 3)
                assertThat(awaitItem())
                    .isInstanceOf<MessageListState.SearchingMessages>()
                    .prop(MessageListState.SearchingMessages::searchQuery).isEqualTo(lastQuery)

                expectNoEvents()
            }
        }

    @Test
    fun `process() should update SearchingMessages's isServerSearch when event is SearchRemotely`() =
        runTest {
            // Arrange
            val messages = createMessageUiItemList(size = 20)
            val stateMachine = createStateMachineOnSearchingMessages(messages = messages)
            advanceUntilIdle()

            stateMachine.currentState.test {
                // enforce correct state before acting.
                assertThat(expectMostRecentItem()).isInstanceOf<MessageListState.SearchingMessages>()

                // Act
                stateMachine.process(MessageListSearchEvent.SearchRemotely)

                // Assert
                assertThat(awaitItem())
                    .isInstanceOf<MessageListState.SearchingMessages>()
                    .prop(MessageListState.SearchingMessages::isServerSearch).isTrue()

                expectNoEvents()
            }
        }

    @Test
    fun `process() should move to LoadedMessages when state is SearchingMessages and event is ExitSearchMode`() =
        runTest {
            // Arrange
            val messages = createMessageUiItemList(size = 20)
            val stateMachine = createStateMachineOnSearchingMessages(messages = messages)
            advanceUntilIdle()

            stateMachine.currentState.test {
                // enforce correct state before acting.
                assertThat(expectMostRecentItem()).isInstanceOf<MessageListState.SearchingMessages>()

                // Act
                stateMachine.process(MessageListSearchEvent.ExitSearchMode)

                // Assert
                assertThat(awaitItem())
                    .isInstanceOf<MessageListState.LoadedMessages>()
                    .prop(MessageListState.LoadedMessages::messages)
                    .isEqualTo(messages)

                expectNoEvents()
            }
        }
    // endregion [SearchingMessages state]

    private suspend fun TestScope.createStateMachineOnLoadingState(
        preferences: MessageListPreferences = createMessageListPreferences(),
        sortTypes: Map<AccountId?, SortType> = mapOf(null to SortType.DateDesc),
        swipeActions: Map<AccountId, SwipeActions> = mapOf(
            AccountIdFactory.create() to SwipeActions(SwipeAction.None, SwipeAction.None),
        ),
    ): MessageListStateMachine {
        val stateMachine = createStateMachine()
        advanceUntilIdle()
        stateMachine.process(event = MessageListEvent.UpdatePreferences(preferences))
        stateMachine.process(event = MessageListEvent.SortTypesLoaded(sortTypes))
        stateMachine.process(event = MessageListEvent.SwipeActionsLoaded(swipeActions))
        stateMachine.process(event = MessageListEvent.AllConfigsReady)
        advanceUntilIdle()
        return stateMachine
    }

    private suspend fun TestScope.createStateMachineOnLoadedState(
        messages: List<MessageItemUi>,
        preferences: MessageListPreferences = createMessageListPreferences(),
        sortTypes: Map<AccountId?, SortType> = mapOf(null to SortType.DateDesc),
        swipeActions: Map<AccountId, SwipeActions> = mapOf(
            AccountIdFactory.create() to SwipeActions(SwipeAction.None, SwipeAction.None),
        ),
    ): MessageListStateMachine {
        val stateMachine = createStateMachine()
        advanceUntilIdle()
        stateMachine.process(event = MessageListEvent.UpdatePreferences(preferences))
        stateMachine.process(event = MessageListEvent.SortTypesLoaded(sortTypes))
        stateMachine.process(event = MessageListEvent.SwipeActionsLoaded(swipeActions))
        stateMachine.process(event = MessageListEvent.AllConfigsReady)
        stateMachine.process(event = MessageListEvent.UpdateLoadingProgress(progress = 1f))
        stateMachine.process(event = MessageListEvent.MessagesLoaded(messages))
        advanceUntilIdle()
        return stateMachine
    }

    private suspend fun TestScope.createStateMachineOnSearchingMessages(
        messages: List<MessageItemUi>,
        preferences: MessageListPreferences = createMessageListPreferences(),
        sortTypes: Map<AccountId?, SortType> = mapOf(null to SortType.DateDesc),
        swipeActions: Map<AccountId, SwipeActions> = mapOf(
            AccountIdFactory.create() to SwipeActions(SwipeAction.None, SwipeAction.None),
        ),
    ): MessageListStateMachine {
        val stateMachine = createStateMachine()
        advanceUntilIdle()
        stateMachine.process(MessageListEvent.UpdatePreferences(preferences))
        stateMachine.process(MessageListEvent.SortTypesLoaded(sortTypes))
        stateMachine.process(MessageListEvent.SwipeActionsLoaded(swipeActions))
        stateMachine.process(event = MessageListEvent.AllConfigsReady)
        stateMachine.process(event = MessageListEvent.UpdateLoadingProgress(progress = 1f))
        stateMachine.process(event = MessageListEvent.MessagesLoaded(messages))
        stateMachine.process(event = MessageListSearchEvent.EnterSearchMode)
        advanceUntilIdle()
        return stateMachine
    }

    private suspend fun TestScope.createStateMachineOnSelectingMessages(
        messages: List<MessageItemUi>,
        preferences: MessageListPreferences = createMessageListPreferences(),
        sortTypes: Map<AccountId?, SortType> = mapOf(null to SortType.DateDesc),
        swipeActions: Map<AccountId, SwipeActions> = mapOf(
            AccountIdFactory.create() to SwipeActions(SwipeAction.None, SwipeAction.None),
        ),
    ): MessageListStateMachine {
        val stateMachine = createStateMachine()
        advanceUntilIdle()
        stateMachine.process(event = MessageListEvent.UpdatePreferences(preferences))
        stateMachine.process(event = MessageListEvent.SortTypesLoaded(sortTypes))
        stateMachine.process(event = MessageListEvent.SwipeActionsLoaded(swipeActions))
        stateMachine.process(event = MessageListEvent.AllConfigsReady)
        stateMachine.process(event = MessageListEvent.UpdateLoadingProgress(progress = 1f))
        stateMachine.process(event = MessageListEvent.MessagesLoaded(messages))
        stateMachine.process(event = MessageListEvent.EnterSelectionMode)
        advanceUntilIdle()
        return stateMachine
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

private fun createMessageUiItemList(
    size: Int,
    accountId: AccountId = AccountIdFactory.create(),
    builder: (index: Int) -> MessageItemUi = { index ->
        when {
            index % 6 == 0 -> createMessageUiItem(
                state = State.Unread,
                id = "id$index",
                accountId = accountId,
            )

            index % 4 == 0 -> createMessageUiItem(
                state = State.Read,
                id = "id$index",
                accountId = accountId,
            )

            index % 2 == 0 -> createMessageUiItem(
                state = State.New,
                id = "id$index",
                accountId = accountId,
            )

            else -> createMessageUiItem(
                state = State.Active,
                id = "id$index",
                accountId = accountId,
            )
        }
    },
): List<MessageItemUi> = List(size) { builder(it) }

private fun createMessageUiItem(
    state: State,
    id: String,
    folderId: String = "mock",
    accountId: AccountId = AccountIdFactory.create(),
    senders: ImmutableList<EmailIdentity> = persistentListOf(),
    recipients: ImmutableList<EmailIdentity> = persistentListOf(),
    subject: String = "mock subject",
    excerpt: String = "mock excerpt",
    formattedReceivedAt: String = "Jan 2026",
    attachments: ImmutableList<MessageItemAttachment> = persistentListOf(),
    starred: Boolean = false,
    encrypted: Boolean = false,
    answered: Boolean = false,
    forwarded: Boolean = false,
    selected: Boolean = false,
    conversations: ImmutableList<MessageItemUi> = persistentListOf(),
): MessageItemUi = MessageItemUi(
    state = state,
    id = id,
    folderId = folderId,
    account = Account(id = accountId, color = Color.Unspecified),
    senders = senders,
    recipients = recipients,
    subject = subject,
    excerpt = excerpt,
    formattedReceivedAt = formattedReceivedAt,
    attachments = attachments,
    starred = starred,
    encrypted = encrypted,
    answered = answered,
    forwarded = forwarded,
    selected = selected,
    conversations = conversations,
)
