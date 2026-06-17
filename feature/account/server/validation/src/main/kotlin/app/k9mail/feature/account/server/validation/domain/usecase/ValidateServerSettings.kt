package app.k9mail.feature.account.server.validation.domain.usecase

import app.k9mail.feature.account.server.validation.domain.ServerValidationDomainContract
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.oauth.AuthStateStorage
import com.fsck.k9.mail.server.ServerSettingsValidationResult
import com.fsck.k9.mail.server.ServerSettingsValidator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.thunderbird.core.preference.GeneralSettingsManager

class ValidateServerSettings(
    private val authStateStorage: AuthStateStorage,
    private val imapValidator: ServerSettingsValidator,
    private val pop3Validator: ServerSettingsValidator,
    private val smtpValidator: ServerSettingsValidator,
    private val generalSettingsManager: GeneralSettingsManager? = null,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ServerValidationDomainContract.UseCase.ValidateServerSettings {
    override suspend fun execute(settings: ServerSettings): ServerSettingsValidationResult {
        return withContext(coroutineDispatcher) {
            val resolvedSettings = generalSettingsManager
                ?.getConfig()
                ?.network
                ?.let(settings::resolveInheritedProxySettings)
                ?: settings

            when (resolvedSettings.type) {
                "imap" -> imapValidator.checkServerSettings(resolvedSettings, authStateStorage)

                "pop3" -> pop3Validator.checkServerSettings(resolvedSettings, authStateStorage)

                "smtp" -> smtpValidator.checkServerSettings(resolvedSettings, authStateStorage)

                "demo" -> ServerSettingsValidationResult.Success

                else -> {
                    throw IllegalArgumentException("Unsupported server type: ${resolvedSettings.type}")
                }
            }
        }
    }
}
