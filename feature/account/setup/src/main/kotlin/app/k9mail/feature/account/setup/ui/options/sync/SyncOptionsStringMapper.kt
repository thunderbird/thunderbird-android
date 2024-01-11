package app.k9mail.feature.account.setup.ui.options.sync

import android.content.res.Resources
import app.k9mail.feature.account.setup.R
import app.k9mail.feature.account.setup.domain.entity.EmailCheckFrequency
import app.k9mail.feature.account.setup.domain.entity.EmailDisplayCount

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
