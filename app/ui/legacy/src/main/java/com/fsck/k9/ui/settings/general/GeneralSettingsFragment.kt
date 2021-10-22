package com.fsck.k9.ui.settings.general

import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.preference.ListPreference
import com.fsck.k9.ui.R
import com.fsck.k9.ui.withArguments
import com.google.android.material.snackbar.Snackbar
import com.takisoft.preferencex.PreferenceFragmentCompat
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.io.IOUtils
import org.koin.android.ext.android.inject

class GeneralSettingsFragment : PreferenceFragmentCompat() {
    private val dataStore: GeneralSettingsDataStore by inject()
    private var rootKey: String? = null

    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore = dataStore
        this.rootKey = rootKey
        setHasOptionsMenu(true)
        setPreferencesFromResource(R.xml.general_settings, rootKey)

        initializeTheme()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activity?.title = preferenceScreen.title
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (rootKey == "debug_preferences") {
            inflater.inflate(R.menu.debug_settings_option, menu)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.exportLogs) {
            exportLogsResultContract.launch("k9mail-logs.txt")
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private val exportLogsResultContract = registerForActivityResult(ActivityResultContracts.CreateDocument()) { uri ->
        if (uri != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                val message = try {
                    requireContext().contentResolver.openOutputStream(uri).use { outputFile ->
                        Runtime.getRuntime().exec("logcat -d").inputStream.use { logOutput ->
                            IOUtils.copy(logOutput, outputFile)
                        }
                    }
                    getString(R.string.debug_export_logs_success)
                } catch (e: IOException) {
                    e.message.toString()
                }
                withContext(Dispatchers.Main) {
                    Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun initializeTheme() {
        (findPreference(PREFERENCE_THEME) as? ListPreference)?.apply {
            if (Build.VERSION.SDK_INT < 28) {
                setEntries(R.array.theme_entries_legacy)
                setEntryValues(R.array.theme_values_legacy)
            }
        }
    }

    companion object {
        private const val PREFERENCE_THEME = "theme"

        fun create(rootKey: String? = null) = GeneralSettingsFragment().withArguments(ARG_PREFERENCE_ROOT to rootKey)
    }
}
