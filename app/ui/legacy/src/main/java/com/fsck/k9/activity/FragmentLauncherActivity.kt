package com.fsck.k9.activity

import android.content.Intent
import android.os.Bundle
import com.fsck.k9.ui.R
import com.fsck.k9.ui.base.K9Activity
import com.fsck.k9.ui.settings.import.SettingsImportFragment
import com.fsck.k9.ui.settings.import.SettingsImportResultViewModel
import com.fsck.k9.ui.settings.import.SettingsImportSuccess
import org.koin.androidx.viewmodel.ext.android.viewModel

class FragmentLauncherActivity : K9Activity() {

    private val settingsImportResultViewModel: SettingsImportResultViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLayout(R.layout.activity_fragment_launcher)

        when (val fragment = intent.getStringExtra(EXTRA_FRAGMENT)) {
            FRAGMENT_IMPORT_SETTINGS -> setupSettingsFragment()
            else -> throw IllegalArgumentException("Unknown destination: $fragment")
        }
    }

    private fun setupSettingsFragment() {
        setTitle(R.string.settings_import_title)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_launcher_container, SettingsImportFragment())
            .commit()

        settingsImportResultViewModel.settingsImportResult.observe(this) {
            if (it == SettingsImportSuccess) {
                launchMessageList()
                finish()
            }
        }
    }

    private fun launchMessageList() {
        val intent = Intent(this, MessageList::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        startActivity(intent)
    }

    companion object {
        const val EXTRA_FRAGMENT = "fragment"
        const val FRAGMENT_IMPORT_SETTINGS = "import_settings"
    }
}
