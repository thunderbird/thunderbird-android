package com.fsck.k9.ui

import android.view.View
import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter
import com.google.android.material.textfield.TextInputLayout

@BindingAdapter("isVisible")
fun setVisibility(view: View, value: Boolean) {
    view.isVisible = value
}

@BindingAdapter("error")
fun setError(view: TextInputLayout, value: Int?) {
    if (value == null) {
        view.error = null
    } else {
        val errorString = view.context.getString(value)
        view.error = errorString
    }
}
