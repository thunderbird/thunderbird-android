@file:JvmName("ContextHelper")

package app.k9mail.core.android.common.activity

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

tailrec fun Context.findActivity(): Activity? {
    return if (this is Activity) {
        this
    } else {
        (this as? ContextWrapper)?.baseContext?.findActivity()
    }
}
