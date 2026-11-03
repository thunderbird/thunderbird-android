package net.thunderbird.feature.mail.message.list.internal.ui.component.template

import androidx.compose.animation.core.snap
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import net.thunderbird.components.ui.bolt.theme.BoltTheme
import net.thunderbird.feature.mail.message.list.internal.ui.component.MessageListItem
import net.thunderbird.feature.mail.message.list.internal.ui.component.organism.MessageListFooter
import net.thunderbird.feature.mail.message.list.internal.ui.component.organism.MessageListSwipeableItem
import net.thunderbird.feature.mail.message.list.ui.component.MessageListScope
import net.thunderbird.feature.mail.message.list.ui.component.ScrollEvent
import net.thunderbird.feature.mail.message.list.ui.event.MessageItemEvent
import net.thunderbird.feature.mail.message.list.ui.event.MessageListEvent
import net.thunderbird.feature.mail.message.list.ui.state.MessageItemUi
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState
import net.thunderbird.feature.mail.message.list.ui.state.PaginationUi

const val TEST_TAG_MESSAGE_LIST_ROOT = "TestMessageList_Root"

@Composable
@Suppress("LongMethod")
internal fun MessageListScope.MessageList(
    state: MessageListState,
    dispatchEvent: (MessageListEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberMessageListLazyState(state, dispatchEvent)
    val listFocusRequester = remember { FocusRequester() }
    var focusRequest by remember { mutableStateOf<ScrollEvent.RevealAndFocusMessage?>(null) }
    var restoredFocusRequest by remember { mutableStateOf<ScrollEvent.RevealAndFocusMessage?>(null) }
    var holdContainerFocus by remember { mutableStateOf(false) }
    val currentState by rememberUpdatedState(state)

    ScrollEventEffect(state, listState, listFocusRequester, onPrepareFocus = { holdContainerFocus = true }) {
        focusRequest = it
        restoredFocusRequest = it
    }
    LaunchedEffect(state.messages, state.preferences, focusRequest) {
        val request = focusRequest ?: return@LaunchedEffect
        if (!state.canRestoreMessageFocus || state.messages.none { it.messageReference == request.messageReference }) {
            focusRequest = null
            request.onFinished()
        }
    }

    LazyColumn(
        modifier = modifier
            .testTag(TEST_TAG_MESSAGE_LIST_ROOT)
            .then(
                if (state.preferences?.isKeypadNavigationEnabled == true) {
                    Modifier
                        .focusRequester(listFocusRequester)
                        .focusProperties { canFocus = holdContainerFocus }
                        .focusable()
                } else {
                    Modifier
                },
            ),
        state = listState,
        contentPadding = PaddingValues(bottom = BoltTheme.sizes.large),
    ) {
        items(
            items = state.messages,
            key = { message -> message.id },
        ) { message ->
            MessageListRow(
                message = message,
                state = state,
                focusRequest = focusRequest,
                dispatchEvent = dispatchEvent,
                onMessageFocus = { holdContainerFocus = false },
                onHandleFocusRequest = {
                    if (focusRequest === it) {
                        focusRequest = null
                        it.onFinished()
                    }
                },
                onFocusedMessageRemove = { reference ->
                    val request = restoredFocusRequest
                    val targetRemoved = request?.messageReference == reference &&
                        currentState.messages.none { it.messageReference == reference }
                    if (targetRemoved && currentState.canRestoreMessageFocus && request?.canFocus() == true) {
                        holdContainerFocus = true
                        listFocusRequester.requestFocus()
                    }
                },
            )
        }

        if (state.metadata.footer.text.isNotBlank()) {
            item {
                MessageListFooter(state, dispatchEvent, Modifier.animateItem(placementSpec = snap()))
            }
        }
    }
}

@Composable
private fun MessageListRow(
    message: MessageItemUi,
    state: MessageListState,
    focusRequest: ScrollEvent.RevealAndFocusMessage?,
    dispatchEvent: (MessageListEvent) -> Unit,
    onMessageFocus: () -> Unit,
    onHandleFocusRequest: (ScrollEvent.RevealAndFocusMessage) -> Unit,
    onFocusedMessageRemove: (String) -> Unit,
) {
    val preferences = state.preferences ?: return
    val focusRequester = rememberMessageFocusRequester(
        message,
        focusRequest,
        state.canRestoreMessageFocus,
        onHandleFocusRequest,
    )
    var hasInputFocus by remember(message.messageReference) { mutableStateOf(false) }
    val latestDispatch by rememberUpdatedState(dispatchEvent)
    DisposableEffect(message.messageReference) {
        onDispose {
            if (hasInputFocus) {
                latestDispatch(MessageItemEvent.OnFocusExit(message))
                onFocusedMessageRemove(message.messageReference)
            }
        }
    }
    val swipeActions = state.metadata.swipeActions[message.account.id]
    MessageListSwipeableItem(message, swipeActions, dispatchEvent) { accessibilityState ->
        MessageListItem(
            message = message,
            showAccountIndicator = state.metadata.showAccountIndicator,
            preferences = preferences,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .semantics(mergeDescendants = true) {
                    stateDescription = accessibilityState.stateDescription(message)
                }
                .onFocusChanged { focusState ->
                    if (hasInputFocus != focusState.hasFocus) {
                        hasInputFocus = focusState.hasFocus
                        if (hasInputFocus) onMessageFocus()
                        dispatchEvent(
                            if (hasInputFocus) {
                                MessageItemEvent.OnFocusEnter(message)
                            } else {
                                MessageItemEvent.OnFocusExit(message)
                            },
                        )
                    }
                },
            onClick = { dispatchEvent(MessageItemEvent.OnMessageClick(message)) },
            onLongClick = { dispatchEvent(MessageItemEvent.ToggleSelectMessages(message)) },
            onAvatarClick = { dispatchEvent(MessageItemEvent.ToggleSelectMessages(message)) },
            onFavouriteClick = { dispatchEvent(MessageItemEvent.ToggleFavourite(message)) },
        )
    }
}

@Composable
private fun rememberMessageFocusRequester(
    message: MessageItemUi,
    request: ScrollEvent.RevealAndFocusMessage?,
    canRestoreFocus: Boolean,
    onHandleRequest: (ScrollEvent.RevealAndFocusMessage) -> Unit,
): FocusRequester {
    val requester = remember(message.messageReference) { FocusRequester() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(request, message.messageReference, canRestoreFocus) {
        if (request == null || request.messageReference != message.messageReference) return@LaunchedEffect
        if (canRestoreFocus && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) && request.canFocus()) {
            requester.requestFocus()
        }
        onHandleRequest(request)
    }
    return requester
}

private val MessageListState.canRestoreMessageFocus: Boolean
    get() = this is MessageListState.LoadedMessages && preferences.isKeypadNavigationEnabled &&
        messages.none { it.selected }

@Composable
private fun rememberMessageListLazyState(
    state: MessageListState,
    dispatchEvent: (MessageListEvent) -> Unit,
): LazyListState {
    val listState = rememberLazyListState()
    val latestPaging by rememberUpdatedState(state.metadata.paging)

    LaunchedEffect(listState, state.metadata.folder?.id) {
        snapshotFlow {
            val total = listState.layoutInfo.totalItemsCount
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible to total
        }
            .distinctUntilChanged()
            .collect { (lastVisible, total) ->
                val hasNextPage = total > 0 && lastVisible >= total - 1 && total > latestPaging.prefetchDistance
                if (!hasNextPage) return@collect
                val prefetchTriggerIndex = (total - 1 - latestPaging.prefetchDistance).coerceAtLeast(0)
                val nearEnd = lastVisible >= prefetchTriggerIndex

                if (nearEnd &&
                    latestPaging.phase != PaginationUi.Phase.Loading &&
                    !latestPaging.endReached
                ) {
                    dispatchEvent(MessageListEvent.LoadNextPage)
                }
            }
    }
    return listState
}

private suspend fun LazyListState.scrollToMessage(
    messages: ImmutableList<MessageItemUi>,
    event: ScrollEvent.ScrollToMessage,
) {
    val (message, animated) = event
    val index = messages.indexOfFirst { message.id == it.id }.takeIf { it >= 0 } ?: return
    val firstVisible = layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0
    val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
    if (index !in firstVisible..lastVisible) {
        if (animated) animateScrollToItem(index) else scrollToItem(index)
    }
}

@Composable
private fun MessageListScope.ScrollEventEffect(
    state: MessageListState,
    listState: LazyListState,
    listFocusRequester: FocusRequester,
    onPrepareFocus: () -> Unit,
    onFocusRequest: (ScrollEvent.RevealAndFocusMessage) -> Unit,
) {
    val currentState by rememberUpdatedState(state)
    val currentFocusRequest by rememberUpdatedState(onFocusRequest)
    val currentPrepareFocus by rememberUpdatedState(onPrepareFocus)
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    val scope = rememberCoroutineScope()
    LifecycleStartEffect(scrollEvents, listState) {
        val job = scope.launch {
            scrollEvents.collect { event ->
                when (event) {
                    is ScrollEvent.ScrollToMessage -> listState.scrollToMessage(currentState.messages, event)

                    is ScrollEvent.RevealAndFocusMessage -> revealAndFocusMessage(
                        event = event,
                        state = { currentState },
                        listState = listState,
                        listFocusRequester = listFocusRequester,
                        lifecycle = lifecycle,
                        prepareFocus = currentPrepareFocus,
                        onFocusRequest = currentFocusRequest,
                    )
                }
            }
        }
        onStopOrDispose {
            job.cancel()
        }
    }

    // For configuration restoration
    LaunchedEffect(listState) {
        val activeMessage = currentState.messages.firstOrNull { it.active }
        if (activeMessage != null) {
            listState.scrollToMessage(
                currentState.messages,
                ScrollEvent.ScrollToMessage(activeMessage, animated = false),
            )
        }
    }
}

private suspend fun revealAndFocusMessage(
    event: ScrollEvent.RevealAndFocusMessage,
    state: () -> MessageListState,
    listState: LazyListState,
    listFocusRequester: FocusRequester,
    lifecycle: Lifecycle,
    prepareFocus: () -> Unit,
    onFocusRequest: (ScrollEvent.RevealAndFocusMessage) -> Unit,
) {
    val isResumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
    if (!state().canRestoreMessageFocus || !isResumed || !event.canFocus()) {
        event.onFinished()
    } else {
        // Clear any previous message focus even when the requested message was removed.
        prepareFocus()
        listFocusRequester.requestFocus()
        val index = state().messages.indexOfFirst { it.messageReference == event.messageReference }
        if (index >= 0) listState.scrollToItem(index)
        if (index >= 0 && state().canRestoreMessageFocus && event.canFocus()) {
            onFocusRequest(event)
        } else {
            event.onFinished()
        }
    }
}
