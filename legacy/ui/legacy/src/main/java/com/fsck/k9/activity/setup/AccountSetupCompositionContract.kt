package com.fsck.k9.activity.setup

import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.core.ui.contract.mvi.UnidirectionalViewModel

interface AccountSetupCompositionContract {

    interface ViewModel : UnidirectionalViewModel<State, Event, Effect>

    @Stable
    data class State(
        val senderName: String,
        val senderEmail: String,
        val bccEmail: String,
        val useSignature: Boolean,
        val signature: String,
        val signatureLocations: PersistentList<Pair<Int, String>>,
        val selectedSignatureLocations: Pair<Int, String>,
    ) {
        companion object {
            val EMPTY: State
                get() = State(
                    senderName = "",
                    senderEmail = "",
                    bccEmail = "",
                    useSignature = false,
                    signature = "",
                    signatureLocations = persistentListOf(),
                    selectedSignatureLocations = Pair(0, ""),
                )
        }
    }

    sealed interface Event {
        data class SenderNameChange(val name: String) : Event
        data class SenderEmailChange(val email: String) : Event
        data class BccEmailChange(val bccEmail: String) : Event
        data class UseSignatureChange(val useSignature: Boolean) : Event
        data class SignatureChange(val signature: String) : Event
        data class SignatureLocationChange(val signatureLocation: Pair<Int, String>) : Event
        data object SavePressed : Event
        data object BackPressed: Event
    }

    sealed interface Effect {
        data class ToggleSaveButtonEnabled(val isEnabled: Boolean) : Effect

        data object DoneUpdatingAccount : Effect
        data object Back: Effect
    }
}
