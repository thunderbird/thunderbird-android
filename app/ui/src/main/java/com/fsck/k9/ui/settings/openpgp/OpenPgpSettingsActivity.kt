package com.fsck.k9.ui.settings.openpgp

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.fsck.k9.activity.K9Activity
import com.fsck.k9.ui.R


/**
 * Based on the official Kotlin example for AndroidX preferences:
 * https://github.com/android/user-interface-samples/blob/master/PreferencesKotlin/app/src/main/java/com/example/androidx/preference/sample/MainActivity.kt
 *
 * Extended by EXTRA_SHOW_FRAGMENT
 */
class OpenPgpSettingsActivity : K9Activity(), PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val fragment = if (intent.hasExtra(EXTRA_SHOW_FRAGMENT)) {
            supportFragmentManager.fragmentFactory.instantiate(
                    classLoader,
                    intent.getStringExtra(EXTRA_SHOW_FRAGMENT)!!
            )
        } else {
            OpenPgpSettingsFragment()
        }

        setContentView(R.layout.general_settings)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        if (savedInstanceState == null) {
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.generalSettingsContainer, fragment)
                    .commit()
        } else {
            title = savedInstanceState.getCharSequence(TITLE_TAG)
        }
        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                setTitle(R.string.encryption_settings_title)
            }
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save current activity title so we can set it again after a configuration change
        outState.putCharSequence(TITLE_TAG, title)
    }

    override fun onSupportNavigateUp(): Boolean {
        if (supportFragmentManager.popBackStackImmediate()) {
            return true
        }
        return super.onSupportNavigateUp()
    }

    override fun onPreferenceStartFragment(
            caller: PreferenceFragmentCompat,
            pref: Preference
    ): Boolean {
        // Instantiate the new Fragment
        val args = pref.extras
        val fragment = supportFragmentManager.fragmentFactory.instantiate(
                classLoader,
                pref.fragment
        ).apply {
            arguments = args
            setTargetFragment(caller, 0)
        }
        // Replace the existing Fragment with the new Fragment
        supportFragmentManager.beginTransaction()
                .replace(R.id.generalSettingsContainer, fragment)
                .addToBackStack(null)
                .commit()
        title = pref.title
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    companion object {
        private const val TITLE_TAG = "settingsActivityTitle"

        const val EXTRA_SHOW_FRAGMENT = "settingsShowFragment"
    }

}
