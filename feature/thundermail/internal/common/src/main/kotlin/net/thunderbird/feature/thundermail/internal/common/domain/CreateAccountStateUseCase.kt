package net.thunderbird.feature.thundermail.internal.common.domain

import app.k9mail.feature.account.common.domain.AccountDomainContract
import app.k9mail.feature.account.common.domain.entity.AccountDisplayOptions
import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.common.domain.entity.AuthorizationState
import com.fsck.k9.mail.ServerSettings
import net.openid.appauth.AuthState
import net.thunderbird.core.outcome.Outcome
import org.json.JSONException

internal class CreateAccountStateUseCase(
    private val accountStateRepository: AccountDomainContract.AccountStateRepository,
) {
    operator fun invoke(
        authorizationState: AuthorizationState,
        incomingServerSettings: ServerSettings,
        outgoingServerSettings: ServerSettings,
    ): Outcome<Unit, Failure> {
        val json = authorizationState.value ?: return Outcome.failure(Failure.AuthorizationStateMissing)
        val (authState, error) = try {
            AuthState.jsonDeserialize(json) to null
        } catch (e: JSONException) {
            null to Failure.InvalidAuthorizationState(error = e)
        }
        val idToken = authState?.parsedIdToken
        val emailAddress = (
            idToken?.additionalClaims["preferred_username"]
                ?: idToken?.additionalClaims["email"]
            )?.toString()

        return when {
            authState == null && error != null -> Outcome.failure(error)
            idToken == null -> Outcome.failure(Failure.IdTokenMissing)
            emailAddress.isNullOrBlank() -> Outcome.failure(Failure.MissingEmail(idToken.toString()))
            else -> {
                val name = (idToken.additionalClaims["name"] ?: idToken.additionalClaims["given_name"])?.toString()
                val state = AccountState(
                    emailAddress = emailAddress,
                    incomingServerSettings = incomingServerSettings.copy(username = emailAddress),
                    outgoingServerSettings = outgoingServerSettings.copy(username = emailAddress),
                    authorizationState = authorizationState,
                    displayOptions = name?.let {
                        AccountDisplayOptions(
                            accountName = emailAddress,
                            displayName = name,
                            emailSignature = null,
                        )
                    },
                )
                accountStateRepository.setState(state)
                Outcome.success(Unit)
            }
        }
    }

    sealed interface Failure {
        data object AuthorizationStateMissing : Failure
        data class InvalidAuthorizationState(val error: Throwable) : Failure
        data object IdTokenMissing : Failure
        data class MissingEmail(val idToken: String) : Failure
    }
}
