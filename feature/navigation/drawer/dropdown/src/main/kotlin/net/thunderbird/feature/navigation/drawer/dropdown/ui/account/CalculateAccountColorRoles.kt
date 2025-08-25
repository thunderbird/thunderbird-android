package net.thunderbird.feature.navigation.drawer.dropdown.ui.account

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import app.k9mail.core.ui.compose.theme2.ColorRoles
import app.k9mail.core.ui.compose.theme2.toColorRoles

/**
 * Calculates the color roles for the given account color.
 *
 * This function is used to derive the color roles for an account based on its color and
 * use remember to avoid unnecessary recomputations.
 *
 * @param accountColor The color of the account.
 */
@Composable
internal fun rememberCalculatedAccountColorRoles(
    accountColor: Color,
): ColorRoles {
    val context = LocalContext.current

    return remember(accountColor) {
        accountColor.toColorRoles(context)
    }
}
