package com.fsck.k9.ui.settings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import app.k9mail.feature.settings.import.ui.SettingsImportFragment
import com.fsck.k9.ui.R
import com.fsck.k9.ui.base.BaseActivity
import com.fsck.k9.ui.base.extensions.findNavController

class SettingsActivity : BaseActivity() {
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLayout(R.layout.activity_settings)

        initializeActionBar()

        if (savedInstanceState == null) {
            handleViewSettingsFileIntent(intent)
        }
    }

    /**
     * Opens the settings import screen with the given file when the app was chosen to open an exported settings file
     * (e.g. from a file manager or cloud storage app).
     */
    private fun handleViewSettingsFileIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_VIEW) return
        val contentUri = intent.data ?: return

        navController.navigate(
            R.id.settingsImportScreen,
            bundleOf(SettingsImportFragment.ARG_CONTENT_URI to contentUri),
        )
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
