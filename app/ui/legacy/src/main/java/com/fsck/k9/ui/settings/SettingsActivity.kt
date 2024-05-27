package com.fsck.k9.ui.settings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.fsck.k9.ui.R
import com.fsck.k9.ui.base.K9Activity
import com.fsck.k9.ui.base.extensions.findNavController

class SettingsActivity : K9Activity() {
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLayout(R.layout.activity_settings)

        initializeActionBar()
    }

    private fun initializeActionBar() {
        // Empty set of top level destinations so the app bar's "up" button is also displayed at the start destination
        val appBarConfiguration = AppBarConfiguration(topLevelDestinationIds = emptySet())

        navController = findNavController(R.id.nav_host_fragment)
        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp() || finishActivity()
    }

    private fun finishActivity(): Boolean {
        finish()
        return true
    }

    companion object {
        @JvmStatic
        fun launch(activity: Activity) {
            val intent = Intent(activity, SettingsActivity::class.java)
            activity.startActivity(intent)
        }
    }
}
