package app.k9mail.feature.account.oauth.data

import app.k9mail.feature.account.common.domain.entity.AuthorizationState
import net.openid.appauth.AuthState
import net.thunderbird.core.logging.legacy.Log
import org.json.JSONException

fun AuthState.toAuthorizationState(): AuthorizationState {
    return try {
        AuthorizationState(value = jsonSerializeString())
    } catch (e: JSONException) {
        Log.e(e, "Error serializing AuthorizationState")
        AuthorizationState()
    }
}

fun AuthorizationState.toAuthState(): AuthState {
    return try {
        value?.let { AuthState.jsonDeserialize(it) } ?: AuthState()
    } catch (e: JSONException) {
        Log.e(e, "Error deserializing AuthorizationState")
        AuthState()
    }
}
