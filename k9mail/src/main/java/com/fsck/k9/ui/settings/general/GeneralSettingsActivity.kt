package com.fsck.k9.ui.settings.general

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.preference.PreferenceFragmentCompat
import android.support.v7.preference.PreferenceFragmentCompat.OnPreferenceStartScreenCallback
import android.support.v7.preference.PreferenceScreen
import android.view.Menu
import android.view.MenuItem
import com.bytehamster.lib.preferencesearch.SearchPreferenceResult
import com.bytehamster.lib.preferencesearch.SearchPreferenceResultListener
import com.fsck.k9.R
import com.fsck.k9.activity.K9Activity
import com.fsck.k9.activity.setup.FontSizeSettings
import com.fsck.k9.ui.fragmentTransaction
import com.fsck.k9.ui.fragmentTransactionWithBackStack
import com.bytehamster.lib.preferencesearch.SearchPreferenceActionView



class GeneralSettingsActivity : K9Activity(), OnPreferenceStartScreenCallback, SearchPreferenceResultListener {
    private lateinit var searchPreferenceActionView: SearchPreferenceActionView
    private lateinit var searchPreferenceMenuItem: MenuItem


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.general_settings)

        initializeActionBar()

        if (savedInstanceState == null) {
            fragmentTransaction {
                add(R.id.generalSettingsContainer, GeneralSettingsFragment.create())
            }
        }
    }

    override fun onSearchResultClicked(result: SearchPreferenceResult) {
        searchPreferenceActionView.close()

        if (result.resourceFile == R.xml.font_preferences) {
            startActivity(Intent(this, FontSizeSettings::class.java))
        } else {
            val fragment = GeneralSettingsFragment.create(result.screen)
            fragmentTransaction {
                addToBackStack("Search result")
                replace(R.id.generalSettingsContainer, fragment)
            }

            result.highlight(fragment as PreferenceFragmentCompat?, 0x009688) // Default material accent color
        }
    }

    private fun initializeActionBar() {
        val actionBar = supportActionBar ?: throw RuntimeException("getSupportActionBar() == null")
        actionBar.setDisplayHomeAsUpEnabled(true)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.general_settings_option, menu)
        searchPreferenceMenuItem = menu.findItem(R.id.search)
        searchPreferenceActionView = searchPreferenceMenuItem.actionView as SearchPreferenceActionView
        searchPreferenceActionView.setActivity(this)
        val config = searchPreferenceActionView.searchConfiguration
        config.setFragmentContainerViewId(R.id.generalSettingsContainer)
        config.setBreadcrumbsEnabled(true)
        config.setFuzzySearchEnabled(true)
        config.index().addFile(R.xml.general_settings)
        config.index().addBreadcrumb(R.string.general_settings_title)
                .addBreadcrumb(R.string.display_preferences)
                .addBreadcrumb(R.string.global_preferences)
                .addBreadcrumb(R.string.font_size_settings_title)
                .addFile(R.xml.font_preferences)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (!searchPreferenceActionView.onBackPressed()) {
            super.onBackPressed()
        }
    }

    override fun onPreferenceStartScreen(
            caller: PreferenceFragmentCompat, preferenceScreen: PreferenceScreen
    ): Boolean {
        fragmentTransactionWithBackStack {
            replace(R.id.generalSettingsContainer, GeneralSettingsFragment.create(preferenceScreen.key))
        }

        return true
    }


    companion object {
        fun start(context: Context) {
            val intent = Intent(context, GeneralSettingsActivity::class.java)
            context.startActivity(intent)
        }
    }
}
