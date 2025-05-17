package app.k9mail.feature.settings.import.ui

import android.app.Activity
import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import app.k9mail.feature.account.oauth.domain.AccountOAuthDomainContract.UseCase.GetOAuthRequestIntent
import app.k9mail.feature.account.oauth.domain.entity.AuthorizationIntentResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.thunderbird.core.android.account.AccountManager
import net.thunderbird.core.android.account.LegacyAccount
import timber.log.Timber

private const val KEY_AUTHORIZATION = "app.k9mail_auth"

@Suppress("TooManyFunctions")
internal class AuthViewModel(
    application: Application,
    private val accountManager: AccountManager,
    private val getOAuthRequestIntent: GetOAuthRequestIntent,
) : AndroidViewModel(application) {
    private var authService: AuthorizationService? = null
    private val authState = AuthState()

    private var account: LegacyAccount? = null

    private lateinit var resultObserver: AppAuthResultObserver

    private val _uiState = MutableStateFlow<AuthFlowState>(AuthFlowState.Idle)
    val uiState: StateFlow<AuthFlowState> = _uiState.asStateFlow()

    @Synchronized
    private fun getAuthService(): AuthorizationService {
        return authService ?: AuthorizationService(getApplication<Application>()).also { authService = it }
    }

    fun init(activityResultRegistry: ActivityResultRegistry, lifecycle: Lifecycle, account: LegacyAccount) {
        this.account = account
        resultObserver = AppAuthResultObserver(activityResultRegistry)
        lifecycle.addObserver(resultObserver)
    }

    fun authResultConsumed() {
        _uiState.update { AuthFlowState.Idle }
    }

    fun isAuthorized(account: LegacyAccount): Boolean {
        val authState = getOrCreateAuthState(account)
        return authState.isAuthorized
    }

    fun isUsingGoogle(account: LegacyAccount): Boolean {
        return GoogleOAuthHelper.isGoogle(account.incomingServerSettings.host!!)
    }

    private fun getOrCreateAuthState(account: LegacyAccount): AuthState {
        return try {
            account.oAuthState?.let { AuthState.jsonDeserialize(it) } ?: AuthState()
        } catch (e: Exception) {
            Timber.e(e, "Error deserializing AuthState")
            AuthState()
        }
    }

    fun login() {
        val account = checkNotNull(account)

        viewModelScope.launch {
            try {
                startLogin(account)
            } catch (e: ActivityNotFoundException) {
                _uiState.update { AuthFlowState.BrowserNotFound }
            }
        }
    }

    private suspend fun startLogin(account: LegacyAccount) {
        val authRequestIntentResult = withContext(Dispatchers.IO) {
            getOAuthRequestIntent.execute(account.incomingServerSettings.host!!, account.email)
        }

        when (authRequestIntentResult) {
            AuthorizationIntentResult.NotSupported -> {
                _uiState.update { AuthFlowState.NotSupported }
            }

            is AuthorizationIntentResult.Success -> resultObserver.login(authRequestIntentResult.intent)
        }
    }

    private fun onLoginResult(authorizationResult: AuthorizationResult?) {
        if (authorizationResult == null) {
            _uiState.update { AuthFlowState.Canceled }
            return
        }

        authorizationResult.response?.let { response ->
            authState.update(authorizationResult.response, authorizationResult.exception)
            exchangeToken(response)
        }

        authorizationResult.exception?.let { authorizationException ->
            _uiState.update {
                AuthFlowState.Failed(
                    errorCode = authorizationException.error,
                    errorMessage = authorizationException.errorDescription,
                )
            }
        }
    }

    private fun exchangeToken(response: AuthorizationResponse) {
        viewModelScope.launch(Dispatchers.IO) {
            val authService = getAuthService()

            val tokenRequest = response.createTokenExchangeRequest()
            authService.performTokenRequest(tokenRequest) { tokenResponse, authorizationException ->
                authState.update(tokenResponse, authorizationException)

                val account = account!!
                account.oAuthState = authState.jsonSerializeString()

                viewModelScope.launch(Dispatchers.IO) {
                    accountManager.saveAccount(account)
                }

                if (authorizationException != null) {
                    _uiState.update {
                        AuthFlowState.Failed(
                            errorCode = authorizationException.error,
                            errorMessage = authorizationException.errorDescription,
                        )
                    }
                } else {
                    _uiState.update { AuthFlowState.Success }
                }
            }
        }
    }

    @Synchronized
    override fun onCleared() {
        authService?.dispose()
        authService = null
    }

    inner class AppAuthResultObserver(private val registry: ActivityResultRegistry) : DefaultLifecycleObserver {
        private var authorizationLauncher: ActivityResultLauncher<Intent>? = null
        private var authRequestIntent: Intent? = null

        override fun onCreate(owner: LifecycleOwner) {
            authorizationLauncher = registry.register(KEY_AUTHORIZATION, AuthorizationContract(), ::onLoginResult)
            authRequestIntent?.let { intent ->
                authRequestIntent = null
                login(intent)
            }
        }

        override fun onDestroy(owner: LifecycleOwner) {
            authorizationLauncher = null
        }

        fun login(authRequestIntent: Intent) {
            val launcher = authorizationLauncher
            if (launcher != null) {
                launcher.launch(authRequestIntent)
            } else {
                this.authRequestIntent = authRequestIntent
            }
        }
    }
}

private class AuthorizationContract : ActivityResultContract<Intent, AuthorizationResult?>() {
    override fun createIntent(context: Context, input: Intent): Intent {
        return input
    }

    override fun parseResult(resultCode: Int, intent: Intent?): AuthorizationResult? {
        return if (resultCode == Activity.RESULT_OK && intent != null) {
            AuthorizationResult(
                response = AuthorizationResponse.fromIntent(intent),
                exception = AuthorizationException.fromIntent(intent),
            )
        } else {
            null
        }
    }
}

private data class AuthorizationResult(
    val response: AuthorizationResponse?,
    val exception: AuthorizationException?,
)

sealed interface AuthFlowState {
    object Idle : AuthFlowState

    object Success : AuthFlowState

    object NotSupported : AuthFlowState

    object BrowserNotFound : AuthFlowState

    object Canceled : AuthFlowState

    data class Failed(val errorCode: String?, val errorMessage: String?) : AuthFlowState {
        override fun toString(): String {
            return listOfNotNull(errorCode, errorMessage).joinToString(separator = " - ")
        }
    }
}
