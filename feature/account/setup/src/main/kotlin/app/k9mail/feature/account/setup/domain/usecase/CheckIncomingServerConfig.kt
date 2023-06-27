package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.feature.account.setup.domain.DomainContract
import app.k9mail.feature.account.setup.domain.entity.IncomingProtocolType
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.server.ServerSettingsValidationResult
import com.fsck.k9.mail.server.ServerSettingsValidator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class CheckIncomingServerConfig(
    private val imapValidator: ServerSettingsValidator,
    private val pop3Validator: ServerSettingsValidator,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : DomainContract.UseCase.CheckIncomingServerConfig {
    override suspend fun execute(
        protocolType: IncomingProtocolType,
        settings: ServerSettings,
    ): ServerSettingsValidationResult = withContext(coroutineDispatcher) {
        return@withContext when (protocolType) {
            IncomingProtocolType.IMAP -> imapValidator.checkServerSettings(settings)
            IncomingProtocolType.POP3 -> pop3Validator.checkServerSettings(settings)
        }
    }
}
