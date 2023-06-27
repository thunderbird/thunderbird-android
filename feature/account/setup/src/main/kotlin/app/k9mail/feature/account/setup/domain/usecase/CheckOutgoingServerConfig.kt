package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.feature.account.setup.domain.DomainContract
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.server.ServerSettingsValidationResult
import com.fsck.k9.mail.server.ServerSettingsValidator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class CheckOutgoingServerConfig(
    private val smtpValidator: ServerSettingsValidator,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : DomainContract.UseCase.CheckOutgoingServerConfig {
    override suspend fun execute(settings: ServerSettings): ServerSettingsValidationResult {
        return withContext(coroutineDispatcher) {
            smtpValidator.checkServerSettings(settings)
        }
    }
}
