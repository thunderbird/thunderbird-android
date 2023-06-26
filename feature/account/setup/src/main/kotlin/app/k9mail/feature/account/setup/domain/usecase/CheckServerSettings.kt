package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.feature.account.setup.domain.DomainContract
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.server.ServerSettingsValidationResult
import com.fsck.k9.mail.server.ServerSettingsValidator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CheckServerSettings(
    private val serverSettingsValidators: Map<String, ServerSettingsValidator>,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : DomainContract.UseCase.CheckServerSettings {
    override suspend fun execute(serverSettings: ServerSettings): ServerSettingsValidationResult {
        val serverSettingsValidator = serverSettingsValidators[serverSettings.type]
            ?: error("Unsupported ServerSettings.type value: ${serverSettings.type}")

        return withContext(coroutineDispatcher) {
            serverSettingsValidator.checkServerSettings(serverSettings)
        }
    }
}
