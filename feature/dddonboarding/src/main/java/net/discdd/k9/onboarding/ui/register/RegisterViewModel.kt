package net.discdd.k9.onboarding.ui.register

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import net.discdd.k9.onboarding.model.RegisterAdu
import net.discdd.k9.onboarding.repository.AuthRepository
import net.discdd.k9.onboarding.repository.AuthRepository.AuthState
import net.discdd.k9.onboarding.ui.register.RegisterContract.State
import net.discdd.k9.onboarding.ui.register.RegisterContract.Event
import net.discdd.k9.onboarding.ui.register.RegisterContract.Effect
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RegisterViewModel(
    initialState: State = State(),
    private val authRepository: AuthRepository
): ViewModel() {
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<State> = _state.asStateFlow()
    private val _effectFlow = MutableSharedFlow<Effect>(replay = 1)
    val effectFlow: SharedFlow<Effect> = _effectFlow.asSharedFlow()

    fun event(event: Event) {
        when (event){
            is Event.Prefix1Changed -> setPrefix1(event.prefix)
            is Event.Prefix2Changed -> setPrefix2(event.prefix)
            is Event.Prefix3Changed -> setPrefix3(event.prefix)
            is Event.Suffix1Changed -> setSuffix1(event.suffix)
            is Event.Suffix2Changed -> setSuffix2(event.suffix)
            is Event.Suffix3Changed -> setSuffix3(event.suffix)
            is Event.PasswordChanged -> setPassword(event.password)
            is Event.OnClickRegister -> register(prefix1=event.prefix1, prefix2=event.prefix2, prefix3=event.prefix3, suffix1=event.suffix1, suffix2=event.suffix2, suffix3=event.suffix3, password = event.password)
        }
    }

    private fun checkAuthState() {
        val (state, ackAdu) = authRepository.getState()
        if (state == AuthState.PENDING){
            viewModelScope.launch {
                Log.d("DDDOnboarding", "emitting")
                _effectFlow.emit(Effect.OnPendingState)
            }
        }
    }

    private fun setPrefix1(prefix: String) {
        _state.update {
            it.copy (
                prefix1 = it.prefix1.updateValue(prefix)
            )
        }
    }

    private fun setPrefix2(prefix: String) {
        _state.update {
            it.copy (
                prefix2 = it.prefix2.updateValue(prefix)
            )
        }
    }

    private fun setPrefix3(prefix: String) {
        _state.update {
            it.copy (
                prefix3 = it.prefix3.updateValue(prefix)
            )
        }
    }

    private fun setSuffix1(suffix: String) {
        _state.update {
            it.copy (
                suffix1 = it.suffix1.updateValue(suffix)
            )
        }
    }

    private fun setSuffix2(suffix: String) {
        _state.update {
            it.copy (
                suffix2 = it.suffix2.updateValue(suffix)
            )
        }
    }

    private fun setSuffix3(suffix: String) {
        _state.update {
            it.copy (
                suffix3 = it.suffix3.updateValue(suffix)
            )
        }
    }

    private fun setPassword(password: String) {
        _state.update {
            it.copy(
                password = it.password.updateValue(password)
            )
        }
    }

    private fun register(prefix1: String, prefix2: String, prefix3: String, suffix1: String, suffix2: String, suffix3: String, password: String) {
        authRepository.insertAdu(RegisterAdu(prefix1=prefix1,prefix2=prefix2,prefix3=prefix3,suffix1=suffix1,suffix2=suffix2,suffix3=suffix3, password=password))
        checkAuthState()
    }
}
