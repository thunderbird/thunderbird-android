package com.fsck.k9

import android.app.Activity
import android.widget.Toast
import androidx.annotation.StringRes

fun Activity.finishWithErrorToast(@StringRes errorRes: Int, vararg formatArgs: String) {
    val text = getString(errorRes, *formatArgs)
    Toast.makeText(this, text, Toast.LENGTH_LONG).show()
    finish()
}
