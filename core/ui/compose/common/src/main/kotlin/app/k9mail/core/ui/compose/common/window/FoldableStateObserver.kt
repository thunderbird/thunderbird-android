package app.k9mail.core.ui.compose.common.window

import android.app.Activity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowLayoutInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import net.thunderbird.core.logging.Logger

/**
 * Observes the foldable state of a device using Jetpack WindowManager.
 *
 * This class tracks fold/unfold events and exposes the current state via a [StateFlow].
 * It automatically handles lifecycle events and provides debouncing to prevent rapid
 * state changes during fold/unfold transitions.
 *
 * @param activity The activity to observe for window layout changes
 * @param logger Logger for debugging
 */
class FoldableStateObserver(
    private val activity: Activity,
    private val logger: Logger,
) : DefaultLifecycleObserver {

    private val windowInfoTracker = WindowInfoTracker.getOrCreate(activity)
    private val scope = CoroutineScope(Dispatchers.Main)
    private var collectJob: Job? = null
    private var debounceJob: Job? = null

    private val _foldableState = MutableStateFlow(lastKnownState)
    val foldableState: StateFlow<FoldableState> = _foldableState.asStateFlow()

    /**
     * Current foldable state value (synchronous access).
     */
    val currentState: FoldableState
        get() = _foldableState.value

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        startObserving()
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        stopObserving()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        cleanup()
    }

    private fun startObserving() {
        logger.debug("FoldableStateObserver") { "Starting to observe window layout info" }

        collectJob = scope.launch {
            windowInfoTracker.windowLayoutInfo(activity)
                .distinctUntilChanged()
                .collect { layoutInfo ->
                    processWindowLayoutInfo(layoutInfo)
                }
        }
    }

    private fun stopObserving() {
        logger.debug("FoldableStateObserver") { "Stopping observation" }
        collectJob?.cancel()
        collectJob = null
        debounceJob?.cancel()
        debounceJob = null
    }

    private fun cleanup() {
        logger.debug("FoldableStateObserver") { "Cleaning up" }
        stopObserving()
    }

    private fun processWindowLayoutInfo(layoutInfo: WindowLayoutInfo) {
        val foldingFeature = layoutInfo.displayFeatures
            .filterIsInstance<FoldingFeature>()
            .firstOrNull()

        val newState = when {
            foldingFeature == null -> {
                // No folding feature means either not a foldable device or unable to detect
                FoldableState.UNKNOWN
            }
            foldingFeature.state == FoldingFeature.State.HALF_OPENED -> {
                // Half-opened state (like a laptop mode) - treat as unfolded for split view
                FoldableState.UNFOLDED
            }
            foldingFeature.state == FoldingFeature.State.FLAT -> {
                // Flat state means fully unfolded
                FoldableState.UNFOLDED
            }
            else -> {
                // Unknown or other states - default to unknown
                FoldableState.UNKNOWN
            }
        }

        logger.verbose("FoldableStateObserver") {
            "Window layout changed: foldingFeature=$foldingFeature, state=${foldingFeature?.state}, newState=$newState"
        }

        // Debounce state changes to prevent rapid toggling during fold/unfold animations
        updateStateWithDebounce(newState)
    }

    private fun updateStateWithDebounce(newState: FoldableState) {
        debounceJob?.cancel()
        debounceJob = scope.launch {
            // Wait 300ms to ensure the fold/unfold gesture is complete
            delay(DEBOUNCE_DELAY_MS)

            if (_foldableState.value != newState) {
                logger.debug("FoldableStateObserver") {
                    "Foldable state changed: ${_foldableState.value} -> $newState"
                }
                lastKnownState = newState
                _foldableState.value = newState
            }
        }
    }

    companion object {
        private const val DEBOUNCE_DELAY_MS = 300L
        private var lastKnownState = FoldableState.UNKNOWN

        internal fun resetStateForTesting() {
            lastKnownState = FoldableState.UNKNOWN
        }
    }
}
