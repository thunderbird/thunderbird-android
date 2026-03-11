/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// TODO(#10676): Delete this file once the `compose-foundation` 1.11.0 is released.
// Since this is a temporary fork, we are going to suppress every detekt lint issue as
// we are going to remove this as soon as the `compose-foundation` 1.11.0 is released
@file:Suppress(
    "LongMethod",
    "LongParameterList",
    "TooManyFunctions",
    "CyclomaticComplexMethod",
    "NestedBlockDepth",
    "ReturnCount",
    "TopLevelPropertyNaming",
    "MagicNumber",
)

package net.thunderbird.core.ui.compose.designsystem.molecule.swipe.fork

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.draggable2D
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFirstOrNull
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.atan2
import kotlin.math.sign
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Configure touch dragging for the UI element in a single [Orientation]. The drag distance reported
 * to [DraggableState], allowing users to react on the drag delta and update their state.
 *
 * The common usecase for this component is when you need to be able to drag something inside the
 * component on the screen and represent this state via one float value
 *
 * If you need to control the whole dragging flow, consider using [pointerInput] instead with the
 * helper functions like [detectDragGestures].
 *
 * If you want to enable dragging in 2 dimensions, consider using [draggable2D].
 *
 * If you are implementing scroll/fling behavior, consider using [scrollable].
 *
 * @sample androidx.compose.foundation.samples.DraggableSample
 * @param state [DraggableState] state of the draggable. Defines how drag events will be interpreted
 *   by the user land logic.
 * @param orientation orientation of the drag
 * @param enabled whether or not drag is enabled
 * @param interactionSource [MutableInteractionSource] that will be used to emit
 *   [DragInteraction.Start] when this draggable is being dragged.
 * @param startDragImmediately when set to true, draggable will start dragging immediately and
 *   prevent other gesture detectors from reacting to "down" events (in order to block composed
 *   press-based gestures). This is intended to allow end users to "catch" an animating widget by
 *   pressing on it. It's useful to set it when value you're dragging is settling / animating.
 * @param onDragStarted callback that will be invoked when drag is about to start at the starting
 *   position, allowing user to suspend and perform preparation for drag, if desired. This suspend
 *   function is invoked with the draggable scope, allowing for async processing, if desired. Note
 *   that the scope used here is the one provided by the draggable node, for long running work that
 *   needs to outlast the modifier being in the composition you should use a scope that fits the
 *   lifecycle needed.
 * @param onDragStopped callback that will be invoked when drag is finished, allowing the user to
 *   react on velocity and process it. This suspend function is invoked with the draggable scope,
 *   allowing for async processing, if desired. Note that the scope used here is the one provided by
 *   the draggable node, for long running work that needs to outlast the modifier being in the
 *   composition you should use a scope that fits the lifecycle needed.
 * @param reverseDirection reverse the direction of the scroll, so top to bottom scroll will behave
 *   like bottom to top and left to right will behave like right to left.
 */
@Stable
internal fun Modifier.draggable(
    state: DraggableState,
    orientation: Orientation,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    startDragImmediately: Boolean = false,
    onDragStarted: suspend CoroutineScope.(startedPosition: Offset) -> Unit = NoOpOnDragStarted,

    onDragStopped: suspend CoroutineScope.(velocity: Float) -> Unit = NoOpOnDragStopped,
    reverseDirection: Boolean = false,
): Modifier = this then DraggableElement(
    state = state,
    orientation = orientation,
    enabled = enabled,
    interactionSource = interactionSource,
    startDragImmediately = startDragImmediately,
    onDragStarted = onDragStarted,
    onDragStopped = onDragStopped,
    reverseDirection = reverseDirection,
)

internal class DraggableElement(
    private val state: DraggableState,
    private val orientation: Orientation,
    private val enabled: Boolean,
    private val interactionSource: MutableInteractionSource?,
    private val startDragImmediately: Boolean,
    private val onDragStarted: suspend CoroutineScope.(startedPosition: Offset) -> Unit,
    private val onDragStopped: suspend CoroutineScope.(velocity: Float) -> Unit,
    private val reverseDirection: Boolean,
) : ModifierNodeElement<DraggableNode>() {
    override fun create(): DraggableNode = DraggableNode(
        state,
        CanDrag,
        orientation,
        enabled,
        interactionSource,
        startDragImmediately,
        onDragStarted,
        onDragStopped,
        reverseDirection,
    )

    override fun update(node: DraggableNode) {
        node.update(
            state,
            CanDrag,
            orientation,
            enabled,
            interactionSource,
            startDragImmediately,
            onDragStarted,
            onDragStopped,
            reverseDirection,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other === null) return false
        if (this::class != other::class) return false

        other as DraggableElement

        if (state != other.state) return false
        if (orientation != other.orientation) return false
        if (enabled != other.enabled) return false
        if (interactionSource != other.interactionSource) return false
        if (startDragImmediately != other.startDragImmediately) return false
        if (onDragStarted != other.onDragStarted) return false
        if (onDragStopped != other.onDragStopped) return false
        if (reverseDirection != other.reverseDirection) return false

        return true
    }

    override fun hashCode(): Int {
        var result = state.hashCode()
        result = 31 * result + orientation.hashCode()
        result = 31 * result + enabled.hashCode()
        result = 31 * result + (interactionSource?.hashCode() ?: 0)
        result = 31 * result + startDragImmediately.hashCode()
        result = 31 * result + onDragStarted.hashCode()
        result = 31 * result + onDragStopped.hashCode()
        result = 31 * result + reverseDirection.hashCode()
        return result
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "draggable"
        properties["orientation"] = orientation
        properties["enabled"] = enabled
        properties["reverseDirection"] = reverseDirection
        properties["interactionSource"] = interactionSource
        properties["startDragImmediately"] = startDragImmediately
        properties["onDragStarted"] = onDragStarted
        properties["onDragStopped"] = onDragStopped
        properties["state"] = state
    }

    companion object {
        val CanDrag: (PointerType) -> Boolean = { true }
    }
}

internal class DraggableNode(
    private var state: DraggableState,
    private var canDrag: (PointerType) -> Boolean,
    private var orientation: Orientation,
    private var enabled: Boolean,
    private var interactionSource: MutableInteractionSource?,
    private var startDragImmediately: Boolean,
    private var onDragStarted: suspend CoroutineScope.(startedPosition: Offset) -> Unit,
    private var onDragStopped: suspend CoroutineScope.(velocity: Float) -> Unit,
    private var reverseDirection: Boolean,
) : DelegatingNode(),
    PointerInputModifierNode,
    CompositionLocalConsumerModifierNode {

    private var channel: Channel<DragEvent>? = null
    private var dragInteraction: DragInteraction.Start? = null
    internal var isListeningForEvents = false
    internal var isListeningForPointerInputEvents = false

    private var pointerInputNode: SuspendingPointerInputModifierNode? = null

    /**
     * Accumulated position offset of this [Modifier.Node] that happened during a drag cycle. This
     * is used to correct the pointer input events that are added to the Velocity Tracker. If this
     * Node is static during the drag cycle, nothing will happen. On the other hand, if the position
     * of this node changes during the drag cycle, we need to correct the Pointer Input used for the
     * drag events, this is because Velocity Tracker doesn't have the knowledge about changes in the
     * position of the container that uses it, and because each Pointer Input event is related to
     * the container's root.
     */
    private var nodeOffset = Offset.Zero

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize,
    ) {
        isListeningForPointerInputEvents = true
        if (enabled && pointerInputNode == null) {
            pointerInputNode = delegate(initializePointerInputNode())
        }
        pointerInputNode?.onPointerEvent(pointerEvent, pass, bounds)
    }

    override fun onCancelPointerInput() {
        pointerInputNode?.onCancelPointerInput()
        isListeningForPointerInputEvents = false
    }

    private fun initializePointerInputNode(): SuspendingPointerInputModifierNode {
        return SuspendingPointerInputModifierNode {
            // re-create tracker when pointer input block restarts. This lazily creates the tracker
            // only when it is need.
            val suspendingPointerInputVelocityTracker = VelocityTracker()
            var previousPositionOnScreen = requireLayoutCoordinates().positionOnScreen()
            val onDragStart:
                (
                    down: PointerInputChange,
                    slopTriggerChange: PointerInputChange,
                    postSlopOffset: Offset,
                ) -> Unit = { down, slopTriggerChange, postSlopOffset ->
                    nodeOffset = Offset.Zero // restart node offset
                    if (canDrag.invoke(down.type)) {
                        if (!isListeningForEvents) startListeningForEvents()
                        suspendingPointerInputVelocityTracker.addPointerInputChange(down)
                        val dragStartedOffset = slopTriggerChange.position - postSlopOffset
                        // the drag start event offset is the down event + touch slop value
                        // or in this case the event that triggered the touch slop minus
                        // the post slop offset
                        channel?.trySend(DragEvent.DragStarted(dragStartedOffset))
                    }
                }

            val onDragEnd: (change: PointerInputChange) -> Unit = { upEvent ->
                suspendingPointerInputVelocityTracker.addPointerInputChange(upEvent)
                val maximumVelocity = viewConfiguration.maximumFlingVelocity
                val velocity =
                    suspendingPointerInputVelocityTracker.calculateVelocity(
                        Velocity(maximumVelocity, maximumVelocity),
                    )
                suspendingPointerInputVelocityTracker.resetTracking()
                channel?.trySend(
                    DragEvent.DragStopped(velocity.toValidVelocity()),
                )
            }

            val onDragCancel: () -> Unit = { channel?.trySend(DragEvent.DragCancelled) }

            val shouldAwaitTouchSlop: () -> Boolean = { !startDragImmediately() }

            val onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit =
                { change, delta ->
                    val currentPositionOnScreen = requireLayoutCoordinates().positionOnScreen()
                    // container changed positions
                    if (currentPositionOnScreen != previousPositionOnScreen) {
                        val delta = currentPositionOnScreen - previousPositionOnScreen
                        nodeOffset += delta
                    }
                    previousPositionOnScreen = currentPositionOnScreen
                    suspendingPointerInputVelocityTracker.addPointerInputChange(
                        event = change,
                        offset = nodeOffset,
                    )
                    channel?.trySend(DragEvent.DragDelta(delta))
                }

            coroutineScope {
                try {
                    var overSlop: Offset
                    awaitEachGesture {
                        val initialDown = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        val awaitTouchSlop = shouldAwaitTouchSlop()
                        if (!awaitTouchSlop) {
                            initialDown.consume()
                        }

                        val down = awaitFirstDown(requireUnconsumed = false)
                        var drag: PointerInputChange?
                        overSlop = Offset.Zero
                        if (awaitTouchSlop) {
                            do {
                                drag = awaitPointerSlopOrCancellation(
                                    pointerId = down.id,
                                    orientation = orientation,
                                ) { change, over ->
                                    change.consume()
                                    overSlop = over
                                }
                            } while (drag != null && !drag.isConsumed)
                        } else {
                            drag = initialDown
                        }

                        // if the pointer is still down, keep reading events in case we need to pick up the gesture.
                        while (drag == null && currentEvent.changes.fastAny { it.pressed }) {
                            var event: PointerEvent
                            do {
                                // use final pass so we only pick up a gesture if it was really ignored by
                                // everyone else
                                event = awaitPointerEvent(pass = PointerEventPass.Final)
                            } while (
                                event.changes.fastAny { it.isConsumed } && event.changes.fastAny { it.pressed }
                            )

                            // an event was not consumed and there's still a pointer in the screen
                            if (event.changes.fastAny { it.pressed }) {
                                // await touch slop again, using the initial down as starting point.
                                // For most cases this should return immediately since we probably moved
                                // far enough from the initial down event.
                                val initialPositionChange =
                                    (event.changes.firstOrNull()?.position ?: Offset.Zero) - down.position
                                drag = awaitPointerSlopOrCancellation(
                                    down.id,
                                    orientation = orientation,
                                    initialPositionChange = initialPositionChange,
                                ) { change, _ ->
                                    change.consume()
                                    // the triggering event will be used as over slop
                                    overSlop = change.positionChange()
                                }
                            }
                        }

                        if (drag != null) {
                            onDragStart.invoke(down, drag, overSlop)
                            onDrag(drag, overSlop)
                            val upEvent =
                                drag(
                                    pointerId = drag.id,
                                    onDrag = {
                                        onDrag(it, it.positionChange())
                                        it.consume()
                                    },
                                    // once drag starts we want to capture drags in any direction, though
                                    // they will be propagated on the correct direction above we want to
                                    // consume any new drag to avoid the cases where we start dragging
                                    // on a given direction and then change directions.
                                    orientation = null,
                                    motionConsumed = { it.isConsumed },
                                )
                            if (upEvent == null) {
                                onDragCancel()
                            } else {
                                onDragEnd(upEvent)
                            }
                        }
                    }
                } catch (cancellation: CancellationException) {
                    channel?.trySend(DragEvent.DragCancelled)
                    if (!isActive) throw cancellation
                }
            }
        }
    }

    private fun startListeningForEvents() {
        isListeningForEvents = true

        if (channel == null) {
            channel = Channel(capacity = Channel.UNLIMITED)
        }

        /**
         * To preserve the original behavior we had (before the Modifier.Node migration) we need to
         * scope the DragStopped and DragCancel methods to the node's coroutine scope instead of
         * using the one provided by the pointer input modifier, this is to ensure that even when
         * the pointer input scope is reset we will continue any coroutine scope scope that we
         * started from these methods while the pointer input scope was active.
         */
        coroutineScope.launch {
            while (isActive) {
                var event = channel?.receive()
                if (event !is DragEvent.DragStarted) continue
                processDragStart(event)
                try {
                    drag { processDelta ->
                        while (event !is DragEvent.DragStopped && event !is DragEvent.DragCancelled) {
                            (event as? DragEvent.DragDelta)?.let(processDelta)
                            event = channel?.receive()
                        }
                    }
                    if (event is DragEvent.DragStopped) {
                        processDragStop(event as DragEvent.DragStopped)
                    } else if (event is DragEvent.DragCancelled) {
                        processDragCancel()
                    }
                } catch (_: CancellationException) {
                    processDragCancel()
                }
            }
        }
    }

    private suspend fun processDragStart(event: DragEvent.DragStarted) {
        dragInteraction?.let { oldInteraction ->
            interactionSource?.emit(DragInteraction.Cancel(oldInteraction))
        }
        val interaction = DragInteraction.Start()
        interactionSource?.emit(interaction)
        dragInteraction = interaction
        onDragStarted(event.startPoint)
    }

    private suspend fun processDragStop(event: DragEvent.DragStopped) {
        dragInteraction?.let { interaction ->
            interactionSource?.emit(DragInteraction.Stop(interaction))
            dragInteraction = null
        }
        onDragStopped(event)
    }

    private suspend fun processDragCancel() {
        dragInteraction?.let { interaction ->
            interactionSource?.emit(DragInteraction.Cancel(interaction))
            dragInteraction = null
        }
        onDragStopped(DragEvent.DragStopped(Velocity.Zero))
    }

    fun disposeInteractionSource() {
        dragInteraction?.let { interaction ->
            interactionSource?.tryEmit(DragInteraction.Cancel(interaction))
            dragInteraction = null
        }
    }

    suspend fun drag(forEachDelta: suspend ((dragDelta: DragEvent.DragDelta) -> Unit) -> Unit) {
        state.drag(MutatePriority.UserInput) {
            forEachDelta { dragDelta ->
                dragBy(dragDelta.delta.reverseIfNeeded().toFloat(orientation))
            }
        }
    }

    fun onDragStarted(startedPosition: Offset) {
        if (!isAttached || onDragStarted == NoOpOnDragStarted) return
        coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            this@DraggableNode.onDragStarted(this, startedPosition)
        }
    }

    fun onDragStopped(event: DragEvent.DragStopped) {
        if (!isAttached || onDragStopped == NoOpOnDragStopped) return
        coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            this@DraggableNode.onDragStopped(
                this,
                event.velocity.reverseIfNeeded().toFloat(orientation),
            )
        }
    }

    fun startDragImmediately(): Boolean = startDragImmediately

    fun update(
        state: DraggableState,
        canDrag: (PointerType) -> Boolean,
        orientation: Orientation,
        enabled: Boolean,
        interactionSource: MutableInteractionSource?,
        startDragImmediately: Boolean,
        onDragStarted: suspend CoroutineScope.(startedPosition: Offset) -> Unit,
        onDragStopped: suspend CoroutineScope.(velocity: Float) -> Unit,
        reverseDirection: Boolean,
    ) {
        var resetPointerInputHandling = false
        if (this.state != state) {
            this.state = state
            resetPointerInputHandling = true
        }
        if (this.orientation != orientation) {
            this.orientation = orientation
            resetPointerInputHandling = true
        }
        if (this.reverseDirection != reverseDirection) {
            this.reverseDirection = reverseDirection
            resetPointerInputHandling = true
        }

        this.onDragStarted = onDragStarted
        this.onDragStopped = onDragStopped
        this.startDragImmediately = startDragImmediately
        if (this.interactionSource != interactionSource) {
            disposeInteractionSource()
            this.interactionSource = interactionSource
        }

        this.canDrag = canDrag
        if (this.enabled != enabled) {
            this.enabled = enabled
            if (!enabled) {
                disposeInteractionSource()
                pointerInputNode?.let { undelegate(it) }
                pointerInputNode = null
            }
            resetPointerInputHandling = true
        }

        if (resetPointerInputHandling) {
            pointerInputNode?.resetPointerInputHandler()
        }
    }

    private fun Velocity.reverseIfNeeded() = if (reverseDirection) this * -1f else this * 1f

    private fun Offset.reverseIfNeeded() = if (reverseDirection) this * -1f else this * 1f
}

internal sealed class DragEvent {
    class DragStarted(val startPoint: Offset) : DragEvent()

    class DragStopped(val velocity: Velocity) : DragEvent()

    object DragCancelled : DragEvent()

    class DragDelta(val delta: Offset) : DragEvent()
}

internal fun Offset.toFloat(orientation: Orientation) =
    if (orientation == Orientation.Vertical) this.y else this.x

private fun Velocity.toFloat(orientation: Orientation) =
    if (orientation == Orientation.Vertical) this.y else this.x

internal fun Velocity.toValidVelocity() =
    Velocity(if (this.x.isNaN()) 0f else this.x, if (this.y.isNaN()) 0f else this.y)

private val NoOpOnDragStarted: suspend CoroutineScope.(startedPosition: Offset) -> Unit = {}
private val NoOpOnDragStopped: suspend CoroutineScope.(velocity: Float) -> Unit = {}

/**
 * Waits for drag motion and uses [orientation] to detect the direction of touch slop detection. It
 * passes [pointerId] as the pointer to examine. If [pointerId] is raised, another pointer from
 * those that are down will be chosen to lead the gesture, and if none are down, `null` is returned.
 * If [pointerId] is not down when [awaitPointerSlopOrCancellation] is called, then `null` is
 * returned.
 *
 * When pointer slop is detected, [onPointerSlopReached] is called with the change and the distance
 * beyond the pointer slop. If [onPointerSlopReached] does not consume the position change, pointer
 * slop will not have been considered detected and the detection will continue or, if it is
 * consumed, the [PointerInputChange] that was consumed will be returned.
 *
 * This works with [awaitTouchSlopOrCancellation] for the other axis to ensure that only horizontal
 * or vertical dragging is done, but not both. It also works for dragging in two ways when using
 * [awaitTouchSlopOrCancellation]
 *
 * We use [initialPositionChange] to consider any amount of initial movement in this gesture before
 * the slop detector is called.
 *
 * @return The [PointerInputChange] of the event that was consumed in [onPointerSlopReached] or
 *   `null` if all pointers are raised or the position change was consumed by another gesture
 *   detector.
 */
internal suspend inline fun AwaitPointerEventScope.awaitPointerSlopOrCancellation(
    pointerId: PointerId,
    orientation: Orientation?,
    initialPositionChange: Offset = Offset.Zero,
    onPointerSlopReached: (PointerInputChange, Offset) -> Unit,
): PointerInputChange? {
    if (currentEvent.isPointerUp(pointerId)) {
        return null // The pointer has already been lifted, so the gesture is canceled
    }
    val touchSlop = viewConfiguration.touchSlop
    var pointer: PointerId = pointerId
    val touchSlopDetector = TouchSlopDetector(orientation, initialPositionChange)
    while (true) {
        val event = awaitPointerEvent()
        val dragEvent = event.changes.fastFirstOrNull { it.id == pointer } ?: return null
        if (dragEvent.isConsumed) {
            return null
        } else if (dragEvent.changedToUpIgnoreConsumed()) {
            val otherDown = event.changes.fastFirstOrNull { it.pressed }
            if (otherDown == null) {
                // This is the last "up"
                return null
            } else {
                pointer = otherDown.id
            }
        } else {
            val postSlopOffset = touchSlopDetector.getPostSlopOffset(
                dragEvent.positionChangeIgnoreConsumed(),
                touchSlop,
            )
            if (postSlopOffset.isSpecified) {
                onPointerSlopReached(dragEvent, postSlopOffset)
                if (dragEvent.isConsumed) {
                    return dragEvent
                } else {
                    touchSlopDetector.reset()
                }
            } else {
                // verify that nothing else consumed the drag event
                awaitPointerEvent(PointerEventPass.Final)
                if (dragEvent.isConsumed) {
                    return null
                }
            }
        }
    }
}

/**
 * Continues to read drag events until all pointers are up or the drag event is canceled. The
 * initial pointer to use for driving the drag is [pointerId]. [onDrag] is called whenever the
 * pointer moves. The up event is returned at the end of the drag gesture.
 *
 * @param pointerId The pointer where that is driving the gesture.
 * @param onDrag Callback for every new drag event.
 * @param motionConsumed If the PointerInputChange should be considered as consumed.
 * @return The last pointer input event change when gesture ended with all pointers up and null when
 *   the gesture was canceled.
 */
internal suspend inline fun AwaitPointerEventScope.drag(
    pointerId: PointerId,
    onDrag: (PointerInputChange) -> Unit,
    orientation: Orientation?,
    motionConsumed: (PointerInputChange) -> Boolean,
): PointerInputChange? {
    if (currentEvent.isPointerUp(pointerId)) {
        return null // The pointer has already been lifted, so the gesture is canceled
    }
    var pointer = pointerId
    while (true) {
        val change =
            awaitDragOrUp(pointer) {
                val positionChange = it.positionChangeIgnoreConsumed()
                val motionChange =
                    if (orientation == null) {
                        positionChange.getDistance()
                    } else {
                        if (orientation == Orientation.Vertical) {
                            positionChange.y
                        } else {
                            positionChange.x
                        }
                    }
                motionChange != 0.0f
            } ?: return null

        if (motionConsumed(change)) {
            return null
        }

        if (change.changedToUpIgnoreConsumed()) {
            return change
        }

        onDrag(change)
        pointer = change.id
    }
}

/**
 * Waits for a single drag in one axis, final pointer up, or all pointers are up. When [pointerId]
 * has lifted, another pointer that is down is chosen to be the finger governing the drag. When the
 * final pointer is lifted, that [PointerInputChange] is returned. When a drag is detected, that
 * [PointerInputChange] is returned. A drag is only detected when [hasDragged] returns `true`.
 *
 * `null` is returned if there was an error in the pointer input stream and the pointer that was
 * down was dropped before the 'up' was received.
 */
private suspend inline fun AwaitPointerEventScope.awaitDragOrUp(
    pointerId: PointerId,
    hasDragged: (PointerInputChange) -> Boolean,
): PointerInputChange? {
    var pointer = pointerId
    while (true) {
        val event = awaitPointerEvent()
        val dragEvent = event.changes.fastFirstOrNull { it.id == pointer } ?: return null
        if (dragEvent.changedToUpIgnoreConsumed()) {
            val otherDown = event.changes.fastFirstOrNull { it.pressed }
            if (otherDown == null) {
                // This is the last "up"
                return dragEvent
            } else {
                pointer = otherDown.id
            }
        } else if (hasDragged(dragEvent)) {
            return dragEvent
        }
    }
}

private fun PointerEvent.isPointerUp(pointerId: PointerId): Boolean =
    changes.fastFirstOrNull { it.id == pointerId }?.pressed != true

/**
 * Detects if touch slop has been crossed after adding a series of [PointerInputChange]. For every
 * new [PointerInputChange] one should add it to this detector using [getPostSlopOffset]. If the
 * position change causes the touch slop to be crossed, [getPostSlopOffset] will return true.
 */
internal class TouchSlopDetector(
    var orientation: Orientation? = null,
    initialPositionChange: Offset = Offset.Zero,
) {

    fun Offset.mainAxis() = if (orientation == Orientation.Horizontal) x else y

    fun Offset.crossAxis() = if (orientation == Orientation.Horizontal) y else x

    /** The accumulation of drag deltas in this detector. */
    private var totalPositionChange: Offset = initialPositionChange

    /**
     * Adds [dragEvent] to this detector. If the accumulated position changes crosses the touch slop
     * provided by [touchSlop], this method will return the post slop offset, that is the total
     * accumulated delta change minus the touch slop value, otherwise this should return null. If
     * [shouldCommit] is true, the delta will be added to the total position change.
     */
    fun getPostSlopOffset(
        positionChange: Offset,
        touchSlop: Float,
        shouldCommit: Boolean = true,
    ): Offset {
        val finalChange = if (shouldCommit) {
            totalPositionChange += positionChange
            totalPositionChange
        } else {
            totalPositionChange + positionChange
        }

        val inDirection = if (orientation == null) {
            finalChange.getDistance()
        } else {
            finalChange.mainAxis().absoluteValue
        }

        val hasCrossedSlop = inDirection >= touchSlop

        return if (hasCrossedSlop && isDeltaAtAngleOfInterest(finalChange)) {
            calculatePostSlopOffset(touchSlop)
        } else {
            Offset.Unspecified
        }
    }

    /**
     * Resets the accumulator associated with this detector.
     *
     * @param initialPositionAccumulator Use to initialize the position change accumulator, for
     *   instance in cases where slop detection may happen "mid-gesture", that is, the slop
     *   detection didn't start from the first down event but somewhere after.
     */
    fun reset(initialPositionAccumulator: Offset = Offset.Zero) {
        totalPositionChange = initialPositionAccumulator
    }

    fun isDeltaAtAngleOfInterest(delta: Offset): Boolean {
        val projectedPositionChange = totalPositionChange + delta
        val angle =
            atan2(
                x = projectedPositionChange.x.absoluteValue,
                y = projectedPositionChange.y.absoluteValue,
            ) * 180 / PI
        return when (orientation) {
            Orientation.Horizontal -> {
                angle < GestureAngleThreshold
            }

            Orientation.Vertical -> {
                angle > GestureAngleThreshold
            }

            else -> {
                false
            }
        }
    }

    private fun calculatePostSlopOffset(touchSlop: Float): Offset {
        return if (orientation == null) {
            val touchSlopOffset =
                totalPositionChange / totalPositionChange.getDistance() * touchSlop
            // update postSlopOffset
            totalPositionChange - touchSlopOffset
        } else {
            val finalMainAxisChange =
                totalPositionChange.mainAxis() - (sign(totalPositionChange.mainAxis()) * touchSlop)
            val finalCrossAxisChange = totalPositionChange.crossAxis()
            if (orientation == Orientation.Horizontal) {
                Offset(finalMainAxisChange, finalCrossAxisChange)
            } else {
                Offset(finalCrossAxisChange, finalMainAxisChange)
            }
        }
    }
}

// An angle in degrees where horizontal and vertical gestures are disambiguated.
private const val GestureAngleThreshold = 30
