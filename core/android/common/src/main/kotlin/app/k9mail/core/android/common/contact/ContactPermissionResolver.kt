package app.k9mail.core.android.common.contact

import android.Manifest.permission.READ_CONTACTS
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import androidx.core.content.ContextCompat

interface ContactPermissionResolver {
    fun hasContactPermission(): Boolean
}

internal class AndroidContactPermissionResolver(private val context: Context) : ContactPermissionResolver {
    override fun hasContactPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, READ_CONTACTS) == PERMISSION_GRANTED
    }
}
