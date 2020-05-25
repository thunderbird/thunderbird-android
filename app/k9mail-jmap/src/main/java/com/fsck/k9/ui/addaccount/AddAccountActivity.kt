package com.fsck.k9.ui.addaccount

import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.fsck.k9.jmap.R
import com.fsck.k9.ui.base.K9Activity
import com.fsck.k9.ui.base.extensions.findNavController

class AddAccountActivity : K9Activity() {
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLayout(R.layout.activity_add_account)

        initializeActionBar()
    }

    private fun initializeActionBar() {
        val appBarConfiguration = AppBarConfiguration(topLevelDestinationIds = setOf(R.id.addJmapAccountScreen))

        navController = findNavController(R.id.nav_host_fragment)
        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
