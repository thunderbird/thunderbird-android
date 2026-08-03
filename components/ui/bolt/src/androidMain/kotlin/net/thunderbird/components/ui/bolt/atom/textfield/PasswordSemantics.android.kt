package net.thunderbird.components.ui.bolt.atom.textfield

import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.password
import androidx.compose.ui.semantics.semantics

internal actual fun Modifier.compatPasswordSemantics(): Modifier {
    /*
     * Workaround for a crash that can occur when the password visibility state changes
     * while an accessibility service is enabled on devices running Android API level 25 or below.
     * This approach mitigates the issue by applying password semantics only on affected versions.
     */
    return semantics {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
            password()
        }
    }
}
