package app.k9mail.feature.account.setup.ui

import android.content.res.Resources
import app.k9mail.core.common.domain.usecase.validation.ValidationError
import app.k9mail.feature.account.setup.R
import app.k9mail.feature.account.setup.domain.entity.ConnectionSecurity
import app.k9mail.feature.account.setup.domain.usecase.ValidateImapPrefix
import app.k9mail.feature.account.setup.domain.usecase.ValidatePassword
import app.k9mail.feature.account.setup.domain.usecase.ValidatePort
import app.k9mail.feature.account.setup.domain.usecase.ValidateServer
import app.k9mail.feature.account.setup.domain.usecase.ValidateUsername

internal fun ConnectionSecurity.toResourceString(resources: Resources): String {
    return when (this) {
        ConnectionSecurity.None -> resources.getString(R.string.account_setup_connection_security_none)
        ConnectionSecurity.StartTLS -> resources.getString(R.string.account_setup_connection_security_start_tls)
        ConnectionSecurity.TLS -> resources.getString(R.string.account_setup_connection_security_ssl)
    }
}

internal fun ValidationError.toResourceString(resources: Resources): String {
    return when (this) {
        is ValidateServer.ValidateServerError -> toServerErrorString(resources)
        is ValidatePort.ValidatePortError -> toPortErrorString(resources)
        is ValidateUsername.ValidateUsernameError -> toUsernameErrorString(resources)
        is ValidatePassword.ValidatePasswordError -> toPasswordErrorString(resources)
        is ValidateImapPrefix.ValidateImapPrefixError -> toImapPrefixErrorString(resources)
        else -> throw IllegalArgumentException("Unknown error: $this")
    }
}

private fun ValidateServer.ValidateServerError.toServerErrorString(resources: Resources): String {
    return when (this) {
        is ValidateServer.ValidateServerError.EmptyServer -> resources.getString(
            R.string.account_setup_validation_error_server_required,
        )
    }
}

private fun ValidatePort.ValidatePortError.toPortErrorString(resources: Resources): String {
    return when (this) {
        is ValidatePort.ValidatePortError.EmptyPort -> resources.getString(
            R.string.account_setup_validation_error_port_required,
        )

        is ValidatePort.ValidatePortError.InvalidPort -> resources.getString(
            R.string.account_setup_validation_error_port_invalid,
        )
    }
}

private fun ValidateUsername.ValidateUsernameError.toUsernameErrorString(resources: Resources): String {
    return when (this) {
        ValidateUsername.ValidateUsernameError.EmptyUsername -> resources.getString(
            R.string.account_setup_validation_error_username_required,
        )
    }
}

private fun ValidatePassword.ValidatePasswordError.toPasswordErrorString(resources: Resources): String {
    return when (this) {
        ValidatePassword.ValidatePasswordError.EmptyPassword -> resources.getString(
            R.string.account_setup_validation_error_password_required,
        )
    }
}

private fun ValidateImapPrefix.ValidateImapPrefixError.toImapPrefixErrorString(resources: Resources): String {
    return when (this) {
        ValidateImapPrefix.ValidateImapPrefixError.BlankImapPrefix -> resources.getString(
            R.string.account_setup_validation_error_imap_prefix_blank,
        )
    }
}
