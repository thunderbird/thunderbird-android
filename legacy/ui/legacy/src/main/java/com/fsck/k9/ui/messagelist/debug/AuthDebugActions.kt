package com.fsck.k9.ui.messagelist.debug

import com.fsck.k9.mail.oauth.AuthStateStorage
import com.fsck.k9.mail.oauth.OAuth2TokenProviderFactory
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.outcome.Outcome

/**
 * Encapsulates debug-only authentication test actions.
 *
 * These methods mutate the stored OAuth state to simulate different scenarios.
 */
class AuthDebugActions(
    private val accountManager: LegacyAccountManager,
    private val oAuth2TokenProviderFactory: OAuth2TokenProviderFactory,
) {
    sealed interface Error {
        data object AccountNotFound : Error
        data object NoOAuthState : Error
        data object CannotModifyAccessToken : Error
        data object AlreadyModified : Error
    }

    fun invalidateAccessTokenLocal(accountUuid: String): Outcome<Unit, Error> {
        val account = accountManager.getAccount(accountUuid)

        return when {
            account == null -> Outcome.failure(Error.AccountNotFound)
            account.oAuthState == null -> Outcome.failure(Error.NoOAuthState)
            else -> {
                val storage = object : AuthStateStorage {
                    override fun getAuthorizationState(): String? = account.oAuthState
                    override fun updateAuthorizationState(authorizationState: String?) {
                        val updated = account.copy(oAuthState = authorizationState)
                        accountManager.saveAccount(updated)
                    }
                }
                val provider = oAuth2TokenProviderFactory.create(storage)
                provider.invalidateToken()
                Outcome.success(Unit)
            }
        }
    }

    fun invalidateAccessTokenServer(accountUuid: String): Outcome<Unit, Error> {
        val account = accountManager.getAccount(accountUuid)
        return when {
            account == null -> Outcome.failure(Error.AccountNotFound)
            account.oAuthState == null -> Outcome.failure(Error.NoOAuthState)
            else -> {
                // Corrupt the serialized access_token value by replacing its value with a known invalid marker.
                val current = account.oAuthState!!
                val regex = "\"access_token\"\\s*:\\s*\"[^\"]*\"".toRegex()
                val modified = regex.replaceFirst(current, "\"access_token\":\"invalid_access_token\"")

                if (modified == current) {
                    return Outcome.failure(Error.AlreadyModified)
                }

                val updated = account.copy(oAuthState = modified)
                accountManager.saveAccount(updated)
                Outcome.success(Unit)
            }
        }
    }

    fun forceAuthFailure(accountUuid: String): Outcome<Unit, Error> {
        val account: LegacyAccount = accountManager.getAccount(accountUuid)
            ?: return Outcome.failure(Error.AccountNotFound)
        // Clear OAuth state to force immediate authentication failure
        val updated = account.copy(oAuthState = null)
        accountManager.saveAccount(updated)
        return Outcome.success(Unit)
    }
}
