package app.k9mail.feature.account.setup.ui.options

import android.content.res.Resources
import app.k9mail.core.common.domain.usecase.validation.ValidationError
import app.k9mail.feature.account.setup.R
import app.k9mail.feature.account.setup.domain.entity.EmailCheckFrequency
import app.k9mail.feature.account.setup.domain.entity.EmailDisplayCount
import app.k9mail.feature.account.setup.domain.usecase.ValidateAccountName.ValidateAccountNameError
import app.k9mail.feature.account.setup.domain.usecase.ValidateAccountName.ValidateAccountNameError.BlankAccountName
import app.k9mail.feature.account.setup.domain.usecase.ValidateDisplayName.ValidateDisplayNameError
import app.k9mail.feature.account.setup.domain.usecase.ValidateDisplayName.ValidateDisplayNameError.EmptyDisplayName
import app.k9mail.feature.account.setup.domain.usecase.ValidateEmailSignature.ValidateEmailSignatureError
import app.k9mail.feature.account.setup.domain.usecase.ValidateEmailSignature.ValidateEmailSignatureError.BlankEmailSignature

internal fun EmailDisplayCount.toResourceString(resources: Resources) = resources.getQuantityString(
    R.plurals.account_setup_options_email_display_count_messages,
    count,
    count,
)

@Suppress("MagicNumber")
internal fun EmailCheckFrequency.toResourceString(resources: Resources): String {
    return when (minutes) {
        -1 -> resources.getString(R.string.account_setup_options_email_check_frequency_never)

        in 1..59 -> resources.getQuantityString(
            R.plurals.account_setup_options_email_check_frequency_minutes,
            minutes,
            minutes,
        )

        else -> resources.getQuantityString(
            R.plurals.account_setup_options_email_check_frequency_hours,
            (minutes / 60),
            (minutes / 60),
        )
    }
}

internal fun ValidationError.toResourceString(resources: Resources): String {
    return when (this) {
        is ValidateAccountNameError -> toAccountNameErrorString(resources)
        is ValidateDisplayNameError -> toDisplayNameErrorString(resources)
        is ValidateEmailSignatureError -> toEmailSignatureErrorString(resources)
        else -> throw IllegalArgumentException("Unknown error: $this")
    }
}

private fun ValidateAccountNameError.toAccountNameErrorString(resources: Resources): String {
    return when (this) {
        is BlankAccountName -> resources.getString(R.string.account_setup_options_account_name_error_blank)
    }
}

private fun ValidateDisplayNameError.toDisplayNameErrorString(resources: Resources): String {
    return when (this) {
        is EmptyDisplayName -> resources.getString(R.string.account_setup_options_display_name_error_required)
    }
}

private fun ValidateEmailSignatureError.toEmailSignatureErrorString(resources: Resources): String {
    return when (this) {
        is BlankEmailSignature -> resources.getString(R.string.account_setup_options_email_signature_error_blank)
    }
}
