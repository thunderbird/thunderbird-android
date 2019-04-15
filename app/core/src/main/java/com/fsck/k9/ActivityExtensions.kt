package com.fsck.k9

import android.app.Activity
import androidx.annotation.StringRes
import android.widget.Toast

fun Activity.finishWithErrorToast(@StringRes errorRes: Int, vararg formatArgs: String) {
    val text = getString(errorRes, *formatArgs)
    Toast.makeText(this, text, Toast.LENGTH_LONG).show()
    finish()
}
