package com.fsck.k9.ui.base.extensions

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction

inline fun FragmentActivity.fragmentTransaction(crossinline block: FragmentTransaction.() -> Unit) {
    with(supportFragmentManager.beginTransaction()) {
        block()
        commit()
    }
}

inline fun FragmentActivity.fragmentTransactionWithBackStack(
    name: String? = null,
    crossinline block: FragmentTransaction.() -> Unit,
) {
    fragmentTransaction {
        block()
        addToBackStack(name)
    }
}

fun <T : Fragment> T.withArguments(block: Bundle.() -> Unit) = apply {
    arguments = Bundle().apply(block)
}
