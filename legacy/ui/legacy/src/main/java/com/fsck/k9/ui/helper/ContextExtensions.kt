@file:JvmName("ContextHelper")

package com.fsck.k9.ui.helper

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

// Source: https://stackoverflow.com/a/58249983
tailrec fun Context.findActivity(): Activity? {
    return this as? Activity ?: (this as? ContextWrapper)?.baseContext?.findActivity()
}
