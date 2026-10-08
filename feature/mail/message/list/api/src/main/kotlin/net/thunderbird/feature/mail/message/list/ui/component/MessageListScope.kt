package net.thunderbird.feature.mail.message.list.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import net.thunderbird.feature.mail.message.list.ui.state.MessageItemUi

/**
 * Scope for controlling the message list from external callers (e.g., effect handlers).
 *
 * Provided as a receiver to composables in the message list tree, allowing scroll commands
 * to reach the [LazyColumn][androidx.compose.foundation.lazy.LazyColumn] without exposing its internal state.
 */
@Stable
interface MessageListScope {
    /**
     * Flow of scroll events to be consumed by the message list composable.
     */
    val scrollEvents: Flow<ScrollEvent>

    /**
     * Requests the message list to scroll to the given [message].
     *
     * @param message The message to scroll to.
     * @param animated Whether to animate the scroll. Defaults to `true`.
     */
    fun scrollToMessage(message: MessageItemUi, animated: Boolean = true)
}

/**
 * One-shot events that control the scroll position of the message list.
 */
sealed interface ScrollEvent {
    /**
     * Requests scrolling to a specific [message].
     *
     * @property message The target message.
     * @property animated Whether the scroll should be animated.
     */
    data class ScrollToMessage(val message: MessageItemUi, val animated: Boolean = true) : ScrollEvent
}

@Stable
internal class DefaultMessageListScope : MessageListScope {
    private val _scrollEvents = Channel<ScrollEvent>(capacity = Channel.CONFLATED)
    override val scrollEvents: Flow<ScrollEvent> = _scrollEvents.receiveAsFlow()

    override fun scrollToMessage(message: MessageItemUi, animated: Boolean) {
        _scrollEvents.trySend(ScrollEvent.ScrollToMessage(message, animated))
    }
}

/**
 * Creates and remembers a [MessageListScope] instance tied to the current composition.
 */
@Composable
fun rememberMessageListScope(): MessageListScope = remember { DefaultMessageListScope() }
