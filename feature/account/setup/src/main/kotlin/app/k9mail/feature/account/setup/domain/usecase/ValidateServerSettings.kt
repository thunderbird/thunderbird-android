package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.feature.account.setup.domain.DomainContract.UseCase
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.oauth.AuthStateStorage
import com.fsck.k9.mail.server.ServerSettingsValidationResult
import com.fsck.k9.mail.server.ServerSettingsValidator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class ValidateServerSettings(
    private val authStateStorage: AuthStateStorage,
    private val imapValidator: ServerSettingsValidator,
    private val pop3Validator: ServerSettingsValidator,
    private val smtpValidator: ServerSettingsValidator,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : UseCase.ValidateServerSettings {
    override suspend fun execute(settings: ServerSettings): ServerSettingsValidationResult {
        return withContext(coroutineDispatcher) {
            when (settings.type) {
                "imap" -> imapValidator.checkServerSettings(settings, authStateStorage)
                "pop3" -> pop3Validator.checkServerSettings(settings, authStateStorage)
                "smtp" -> smtpValidator.checkServerSettings(settings, authStateStorage)
                else -> {
                    throw IllegalArgumentException("Unsupported server type: ${settings.type}")
                }
            }
        }
    }
}
