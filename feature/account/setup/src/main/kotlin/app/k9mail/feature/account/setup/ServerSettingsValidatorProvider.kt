package app.k9mail.feature.account.setup

import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.server.ServerSettingsValidator

/**
 * Provides a mapping from the protocol value used in [ServerSettings.type] to a [ServerSettingsValidator].
 *
 * Note: Apps using this feature module need to provide an instance of this interface via Koin.
 */
fun interface ServerSettingsValidatorProvider {
    fun getValidators(): Map<String, ServerSettingsValidator>
}
