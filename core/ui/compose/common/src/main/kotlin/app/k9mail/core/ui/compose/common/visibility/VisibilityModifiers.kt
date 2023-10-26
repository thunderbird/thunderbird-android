package app.k9mail.core.ui.compose.common.visibility

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha

/**
 * Sets a composable to be fully transparent when it should be hidden (but still present for layout purposes).
 */
fun Modifier.hide(hide: Boolean) = alpha(if (hide) 0f else 1f)
