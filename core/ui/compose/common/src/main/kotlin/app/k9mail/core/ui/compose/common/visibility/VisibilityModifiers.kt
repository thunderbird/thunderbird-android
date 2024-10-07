package app.k9mail.core.ui.compose.common.visibility

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.clearAndSetSemantics

/**
 * Sets a composable to be fully transparent when it should be hidden (but still present for layout purposes).
 */
fun Modifier.hide(hide: Boolean): Modifier {
    return if (hide) {
        alpha(0f).clearAndSetSemantics {}
    } else {
        alpha(1f)
    }
}
