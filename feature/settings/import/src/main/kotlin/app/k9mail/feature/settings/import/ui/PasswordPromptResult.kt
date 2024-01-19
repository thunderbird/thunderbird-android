package app.k9mail.feature.settings.import.ui

import android.content.Intent

internal data class PasswordPromptResult(
    val accountUuid: String,
    val incomingServerPassword: String?,
    val outgoingServerPassword: String?,
) {
    fun asIntent() = Intent().apply {
        putExtra(EXTRA_ACCOUNT_UUID, accountUuid)
        putExtra(EXTRA_INCOMING_SERVER_PASSWORD, incomingServerPassword)
        putExtra(EXTRA_OUTGOING_SERVER_PASSWORD, outgoingServerPassword)
    }

    companion object {
        private const val EXTRA_ACCOUNT_UUID = "accountUuid"
        private const val EXTRA_INCOMING_SERVER_PASSWORD = "incomingServerPassword"
        private const val EXTRA_OUTGOING_SERVER_PASSWORD = "outgoingServerPassword"

        fun fromIntent(intent: Intent): PasswordPromptResult {
            val accountUuid = intent.getStringExtra(EXTRA_ACCOUNT_UUID) ?: error("Missing account UUID")
            val incomingServerPassword = intent.getStringExtra(EXTRA_INCOMING_SERVER_PASSWORD)
            val outgoingServerPassword = intent.getStringExtra(EXTRA_OUTGOING_SERVER_PASSWORD)

            return PasswordPromptResult(accountUuid, incomingServerPassword, outgoingServerPassword)
        }
    }
}
