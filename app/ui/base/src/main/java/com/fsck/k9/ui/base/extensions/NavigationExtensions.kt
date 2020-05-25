package com.fsck.k9.ui.base.extensions

import androidx.annotation.IdRes
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment

fun FragmentActivity.findNavController(@IdRes containerIdRes: Int): NavController {
    val navHostFragment = supportFragmentManager.findFragmentById(containerIdRes) as NavHostFragment
    return navHostFragment.navController
}
