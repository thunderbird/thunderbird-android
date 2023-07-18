package app.k9mail.feature.account.oauth.domain.entity

import android.content.Intent

sealed interface AuthorizationIntentResult {
    object NotSupported : AuthorizationIntentResult

    data class Success(
        val intent: Intent,
    ) : AuthorizationIntentResult
}
