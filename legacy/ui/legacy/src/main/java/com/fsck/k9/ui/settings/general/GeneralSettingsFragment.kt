package com.fsck.k9.ui.settings.general

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceScreen
import app.k9mail.feature.telemetry.api.TelemetryManager
import com.fsck.k9.FileLoggerTree
import com.fsck.k9.ui.R
import com.fsck.k9.ui.base.extensions.withArguments
import com.fsck.k9.ui.observe
import com.fsck.k9.ui.settings.remove
import com.google.android.material.snackbar.Snackbar
import com.takisoft.preferencex.PreferenceFragmentCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import net.thunderbird.core.featureflag.FeatureFlagProvider
import net.thunderbird.core.featureflag.toFeatureFlagKey
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class GeneralSettingsFragment : PreferenceFragmentCompat() {
    private val viewModel: GeneralSettingsViewModel by viewModel()
    private val dataStore: GeneralSettingsDataStore by inject()
    private val telemetryManager: TelemetryManager by inject()
    private val featureFlagProvider: FeatureFlagProvider by inject()

    private var rootKey: String? = null
    private var currentUiState: GeneralSettingsUiState? = null
    private var snackbar: Snackbar? = null

    private val exportLogsResultContract = registerForActivityResult(CreateDocument("text/plain")) { contentUri ->
        if (contentUri != null) {
            viewModel.exportLogs(contentUri)
        }
    }

    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore = dataStore
        this.rootKey = rootKey
        setHasOptionsMenu(true)
        setPreferencesFromResource(R.xml.general_settings, rootKey)
        val listener = Preference.OnPreferenceChangeListener { _, newValue ->
            if (!(newValue as Boolean)) {
                val now = Calendar.getInstance()
                val uriString = String.format(
                    Locale.US,
                    "%s_%s.txt",
                    FileLoggerTree.DEFAULT_SYNC_FILENAME,
                    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now.time),
                )
                exportLogsResultContract.launch(uriString)
            }
            true
        }
        findPreference<Preference>("sync_debug_logging")?.onPreferenceChangeListener = listener
        featureFlagProvider.provide("disable_font_size_config".toFeatureFlagKey())
            .onEnabled {
                val parentPreference = findPreference<PreferenceCategory>("global_preferences")
                val fontSizePreferenceScreen = findPreference<PreferenceScreen>("font_size")

                if (parentPreference != null && fontSizePreferenceScreen != null) {
                    parentPreference.removePreference(fontSizePreferenceScreen)
                }
            }

        initializeDataCollection()

        viewModel.uiState.observe(this) { uiState ->
            updateUiState(uiState)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        dismissSnackbar()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.title = preferenceScreen.title
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (rootKey == PREFERENCE_SCREEN_DEBUGGING) {
            inflater.inflate(R.menu.debug_settings_option, menu)
            currentUiState?.let { uiState ->
                menu.findItem(R.id.exportLogs).isEnabled = uiState.isExportLogsMenuEnabled
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.exportLogs) {
            exportLogsResultContract.launch(GeneralSettingsViewModel.DEFAULT_FILENAME)
        } else if (item.itemId == R.id.exportSyncLogs) {
            val now = Calendar.getInstance()
            val uriString = String.format(
                Locale.US,
                "%s_%s.txt",
                FileLoggerTree.DEFAULT_SYNC_FILENAME,
                SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now.time),
            )
            exportLogsResultContract.launch(uriString)
        }

        return super.onOptionsItemSelected(item)
    }

    private fun initializeDataCollection() {
        if (!telemetryManager.isTelemetryFeatureIncluded()) {
            findPreference<Preference>(PREFERENCE_DATA_COLLECTION)?.remove()
        }
    }

    private fun updateUiState(uiState: GeneralSettingsUiState) {
        val oldUiState = currentUiState
        currentUiState = uiState

        if (oldUiState?.isExportLogsMenuEnabled != uiState.isExportLogsMenuEnabled) {
            setExportLogsMenuEnabled()
        }

        if (oldUiState?.snackbarState != uiState.snackbarState) {
            setSnackbarState(uiState.snackbarState)
        }
    }

    private fun setExportLogsMenuEnabled() {
        requireActivity().invalidateOptionsMenu()
    }

    private fun setSnackbarState(snackbarState: SnackbarState) {
        when (snackbarState) {
            SnackbarState.Hidden -> dismissSnackbar()
            SnackbarState.ExportLogSuccess -> showSnackbar(R.string.debug_export_logs_success)
            SnackbarState.ExportLogFailure -> showSnackbar(R.string.debug_export_logs_failure)
        }
    }

    private fun dismissSnackbar() {
        snackbar?.dismiss()
        snackbar = null
    }

    private fun showSnackbar(message: Int) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_INDEFINITE)
            .also { snackbar = it }
            .show()
    }

    companion object {
        private const val PREFERENCE_SCREEN_DEBUGGING = "debug_preferences"
        private const val PREFERENCE_DATA_COLLECTION = "data_collection"

        fun create(rootKey: String? = null) = GeneralSettingsFragment().withArguments(ARG_PREFERENCE_ROOT to rootKey)
    }
}
