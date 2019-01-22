package com.fsck.k9.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction
import androidx.core.os.bundleOf

inline fun androidx.fragment.app.FragmentActivity.fragmentTransaction(crossinline block: androidx.fragment.app.FragmentTransaction.() -> Unit) {
    with(supportFragmentManager.beginTransaction()) {
        block()
        commit()
    }
}

inline fun androidx.fragment.app.FragmentActivity.fragmentTransactionWithBackStack(
        name: String? = null,
        crossinline block: androidx.fragment.app.FragmentTransaction.() -> Unit
) {
    fragmentTransaction {
        block()
        addToBackStack(name)
    }
}

fun androidx.fragment.app.Fragment.withArguments(vararg argumentPairs: Pair<String, Any?>) = apply {
    arguments = bundleOf(*argumentPairs)
}
