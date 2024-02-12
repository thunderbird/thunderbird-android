package app.k9mail.core.android.common.compat

import android.os.Build
import android.os.Bundle
import java.io.Serializable

// This class resolves a deprecation warning and issue with the Bundle.getSerializable method
// Fixes https://issuetracker.google.com/issues/314250395
// Could be removed once releases in androidx.core.os.BundleCompat
object BundleCompat {

    @JvmStatic
    fun <T : Serializable> getSerializable(bundle: Bundle, key: String?, clazz: Class<T>): T? = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> bundle.getSerializable(key, clazz)
        else -> {
            @Suppress("DEPRECATION")
            val serializable = bundle.getSerializable(key)
            @Suppress("UNCHECKED_CAST")
            if (clazz.isInstance(serializable)) serializable as T else null
        }
    }
}
