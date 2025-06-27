package net.thunderbird.feature.navigation.drawer.dropdown.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.feature.navigation.drawer.dropdown.R
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayAccount
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.MailDisplayAccount
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.UnifiedDisplayAccount

@Composable
internal fun getDisplayAccountColor(account: DisplayAccount): Color {
    return when (account) {
        is UnifiedDisplayAccount -> {
            MainTheme.colors.onSurfaceVariant
        }
        is MailDisplayAccount -> {
            Color(account.color)
        }

        else -> throw IllegalArgumentException("Unknown account type: ${account::class.java.simpleName}")
    }
}

@Composable
internal fun getDisplayAccountName(account: DisplayAccount): String {
    return when (account) {
        is UnifiedDisplayAccount -> {
            stringResource(R.string.navigation_drawer_dropdown_unified_account_title)
        }
        is MailDisplayAccount -> {
            account.name
        }

        else -> throw IllegalArgumentException("Unknown account type: ${account::class.java.simpleName}")
    }
}
