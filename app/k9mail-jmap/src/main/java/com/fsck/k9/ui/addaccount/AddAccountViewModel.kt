package com.fsck.k9.ui.addaccount

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fsck.k9.EmailAddressValidator
import com.fsck.k9.backend.jmap.JmapAccountDiscovery
import com.fsck.k9.backend.jmap.JmapDiscoveryResult
import com.fsck.k9.backend.jmap.JmapDiscoveryResult.JmapAccount
import com.fsck.k9.backends.JmapAccountCreator
import com.fsck.k9.helper.SingleLiveEvent
import com.fsck.k9.helper.measureRealtimeMillisWithResult
import com.fsck.k9.jmap.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddAccountViewModel(
    private val emailAddressValidator: EmailAddressValidator,
    private val jmapAccountDiscovery: JmapAccountDiscovery,
    private val jmapAccountCreator: JmapAccountCreator
) : ViewModel() {
    val emailAddress = MutableLiveData<String>()
    val emailAddressError = MutableLiveData<Int?>()
    val password = MutableLiveData<String>()
    val passwordError = MutableLiveData<Int?>()
    val setupErrorText: MutableLiveData<Int> = createMutableLiveData(R.string.empty_string)
    val isInputEnabled: MutableLiveData<Boolean> = createMutableLiveData(true)
    val isNextButtonEnabled: MutableLiveData<Boolean> = createMutableLiveData(true)
    val isProgressBarVisible: MutableLiveData<Boolean> = createMutableLiveData(false)
    private val actionLiveData = SingleLiveEvent<Action>()

    init {
        Transformations.distinctUntilChanged(emailAddress).observeForever { resetEmailAddressError() }
        Transformations.distinctUntilChanged(password).observeForever { resetPasswordError() }
    }

    fun getActionEvents(): LiveData<Action> = actionLiveData

    fun onNextButtonClicked() {
        discoverServerSettings()
    }

    private fun discoverServerSettings() {
        val emailAddress = this.emailAddress.value?.trim() ?: ""
        val password = this.password.value ?: ""

        if (!emailAddressValidator.isValidAddressOnly(emailAddress)) {
            displayEmailAddressError(R.string.add_account__email_address_error)
            return
        }

        showDiscoveryProgressBar()

        viewModelScope.launch {
            val (elapsed, discoveryResult) = measureRealtimeMillisWithResult {
                withContext(Dispatchers.IO) {
                    jmapAccountDiscovery.discover(emailAddress, password)
                }
            }

            if (elapsed < MIN_PROGRESS_DURATION) {
                delay(MIN_PROGRESS_DURATION - elapsed)
            }

            if (discoveryResult is JmapAccount) {
                createAccount(emailAddress, password, discoveryResult)
            } else {
                displayDiscoveryError(discoveryResult)
                hideDiscoveryProgressBar()
            }
        }
    }

    private suspend fun createAccount(emailAddress: String, password: String, jmapAccount: JmapAccount) {
        GlobalScope.launch(Dispatchers.IO) {
            jmapAccountCreator.createAccount(emailAddress, password, jmapAccount)
        }.join()

        sendActionEvent(Action.GoToMessageList)
    }

    private fun displayDiscoveryError(discoveryResult: JmapDiscoveryResult) {
        when (discoveryResult) {
            is JmapDiscoveryResult.GenericFailure -> {
                displayError(R.string.add_account__generic_failure)
            }
            is JmapDiscoveryResult.NoEmailAccountFoundFailure -> {
                displayError(R.string.add_account__no_email_account_found)
            }
            is JmapDiscoveryResult.AuthenticationFailure -> {
                displayPasswordError(R.string.add_account__password_error)
            }
            is JmapDiscoveryResult.EndpointNotFoundFailure -> {
                displayError(R.string.add_account__jmap_server_not_found)
            }
        }
    }

    @Suppress("SameParameterValue")
    private fun displayEmailAddressError(@StringRes error: Int) {
        emailAddressError.value = error
    }

    private fun resetEmailAddressError() {
        emailAddressError.value = null
    }

    @Suppress("SameParameterValue")
    private fun displayPasswordError(@StringRes error: Int) {
        passwordError.value = error
    }

    private fun resetPasswordError() {
        passwordError.value = null
    }

    private fun showDiscoveryProgressBar() {
        isInputEnabled.value = false
        isProgressBarVisible.value = true
        isNextButtonEnabled.value = false
        setupErrorText.value = R.string.empty_string
    }

    private fun hideDiscoveryProgressBar() {
        isInputEnabled.value = true
        isProgressBarVisible.value = false
        isNextButtonEnabled.value = true
    }

    private fun displayError(@StringRes error: Int) {
        setupErrorText.value = error
    }

    private fun sendActionEvent(action: Action) {
        actionLiveData.value = action
    }

    private fun <T> createMutableLiveData(initialValue: T): MutableLiveData<T> {
        return MutableLiveData<T>().apply {
            value = initialValue
        }
    }

    companion object {
        private const val MIN_PROGRESS_DURATION = 500
    }
}

sealed class Action {
    object GoToMessageList : Action()
}
