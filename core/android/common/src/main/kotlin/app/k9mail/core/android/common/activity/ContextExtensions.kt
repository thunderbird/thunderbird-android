@file:JvmName("ContextHelper")

package app.k9mail.core.android.common.activity

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

// Source: https://stackoverflow.com/a/58249983
tailrec fun Context.findActivity(): Activity? = this as? Activity
    ?: (this as? ContextWrapper)?.baseContext?.findActivity()
