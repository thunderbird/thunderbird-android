package app.k9mail.feature.account.oauth.data

import app.k9mail.feature.account.common.domain.entity.AuthorizationState
import net.openid.appauth.AuthState
import org.json.JSONException
import timber.log.Timber

fun AuthState.toAuthorizationState(): AuthorizationState {
    return try {
        AuthorizationState(value = jsonSerializeString())
    } catch (e: JSONException) {
        Timber.e(e, "Error serializing AuthorizationState")
        AuthorizationState()
    }
}

fun AuthorizationState.toAuthState(): AuthState {
    return try {
        value?.let { AuthState.jsonDeserialize(it) } ?: AuthState()
    } catch (e: JSONException) {
        Timber.e(e, "Error deserializing AuthorizationState")
        AuthState()
    }
}
