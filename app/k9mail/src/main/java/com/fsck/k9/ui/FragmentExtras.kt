package com.fsck.k9.ui

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.app.FragmentTransaction
import androidx.core.os.bundleOf

inline fun FragmentActivity.fragmentTransaction(crossinline block: FragmentTransaction.() -> Unit) {
    with(supportFragmentManager.beginTransaction()) {
        block()
        commit()
    }
}

inline fun FragmentActivity.fragmentTransactionWithBackStack(
        name: String? = null,
        crossinline block: FragmentTransaction.() -> Unit
) {
    fragmentTransaction {
        block()
        addToBackStack(name)
    }
}

fun Fragment.withArguments(vararg argumentPairs: Pair<String, Any?>) = apply {
    arguments = bundleOf(*argumentPairs)
}
