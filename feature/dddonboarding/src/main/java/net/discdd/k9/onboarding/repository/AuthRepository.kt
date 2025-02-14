package net.discdd.k9.onboarding.repository

import android.net.Uri

interface AuthRepository {
    enum class AuthState {
        LOGGED_IN,
        PENDING,
        LOGGED_OUT
    }

    val CONTENT_URL: Uri;

    fun getState(): Pair<AuthState, net.discdd.k9.onboarding.model.AcknowledgementAdu?>

    fun setState(state: AuthState)

    fun insertAdu(adu: net.discdd.k9.onboarding.model.Adu): Boolean
}
