package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.feature.account.setup.domain.DomainContract.UseCase
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.server.ServerSettingsValidationResult
import com.fsck.k9.mail.server.ServerSettingsValidator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class ValidateServerSettings(
    private val imapValidator: ServerSettingsValidator,
    private val pop3Validator: ServerSettingsValidator,
    private val smtpValidator: ServerSettingsValidator,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : UseCase.ValidateServerSettings {
    override suspend fun execute(settings: ServerSettings): ServerSettingsValidationResult =
        withContext(coroutineDispatcher) {
            return@withContext when (settings.type) {
                "imap" -> imapValidator.checkServerSettings(settings)
                "pop3" -> pop3Validator.checkServerSettings(settings)
                "smtp" -> smtpValidator.checkServerSettings(settings)
                else -> {
                    throw IllegalArgumentException("Unsupported server type: ${settings.type}")
                }
            }
        }
}
