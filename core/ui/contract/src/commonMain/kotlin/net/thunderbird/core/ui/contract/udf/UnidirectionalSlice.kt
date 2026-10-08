package net.thunderbird.core.ui.contract.udf

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * A contract for a unidirectional slice of a view model that follows the Model-View-Intent (MVI) architecture pattern.
 * A slice is a part of the view model that manages a specific feature or screen. It has its own state, events, and
 * effects. It can be used to modularize the view model and make it easier to test and maintain. The slice can be
 * combined with other slices to create a complete view model for a screen or feature.
 *
 * @param STATE The type that represents the state of the slice. For example, the UI state of a screen can be
 * represented as a state.
 * @param EVENT The type that represents user actions that can occur and should be handled by the slice. For
 * example, a button click can be represented as an event.
 * @param EFFECT The type that represents side-effects that can occur in response to the state changes. For example,
 * a navigation event can be represented as an effect.
 */
public interface UnidirectionalSlice<STATE, EVENT, EFFECT> {

    /**
     * The current [STATE] of the view model.
     */
    public val state: StateFlow<STATE>

    /**
     * The side-effects ([EFFECT]) produced by the slice.
     */
    public val effect: SharedFlow<EFFECT>

    /**
     * Handles an [EVENT] and updates the [STATE] of the view model.
     *
     * @param event The [EVENT] to handle.
     */
    public fun event(event: EVENT)
}
