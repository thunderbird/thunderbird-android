package com.fsck.k9.ui.settings.openpgp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.preference.Preference
import androidx.recyclerview.widget.RecyclerView
import com.fsck.k9.Account
import com.fsck.k9.Identity
import com.fsck.k9.Preferences
import com.fsck.k9.ui.R
import com.fsck.k9.ui.observeNotNull
import com.fsck.k9.ui.settings.general.GeneralSettingsDataStore
import com.fsck.k9.ui.withArguments
import com.takisoft.preferencex.PreferenceCategory
import com.takisoft.preferencex.PreferenceFragmentCompat
import com.takisoft.preferencex.SwitchPreferenceCompat
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.sufficientlysecure.keychain.ui.MainActivity

class OpenPgpSettingsFragment : PreferenceFragmentCompat() {
    private val preferences: Preferences by inject()
    private val dataStore: GeneralSettingsDataStore by inject()

    private val viewModel: OpenPgpSettingsViewModel by viewModel()

    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore = dataStore
        setPreferencesFromResource(R.xml.openpgp_settings, rootKey)

        viewModel.accounts.observeNotNull(this) { accounts ->
            populateIdentitiesList(accounts)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dataStore.activity = activity
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        return when (preference!!.key) {
            KEY_MANAGE -> {
                showManageKeys()
                true
            }
            else -> super.onPreferenceTreeClick(preference)
        }
    }

    private fun showManageKeys() {
        val intent = Intent(context, MainActivity::class.java)
        startActivity(intent)
    }

    private fun populateIdentitiesList(accounts: List<Account>) {
        val identityCategory = findPreference<PreferenceCategory>(KEY_CATEGORY_IDENTITIES)!!

        for (account in accounts) {
            for ((identityIndex, identity) in account.identities.withIndex()) {
                addIdentityPreference(account, identityIndex, identity, identityCategory)
            }
        }
    }

    private fun addIdentityPreference(account: Account, identityIndex: Int, identity: Identity, preferenceCategory: PreferenceCategory) {
        val uniqueIdentityPrefKey = "identity_${account.uuid}_${identityIndex}"
        var identityPref = preferenceCategory.findPreference<SwitchPreferenceCompat>(uniqueIdentityPrefKey)
        if (identityPref == null) {
            identityPref = SwitchPreferenceCompat(context)
            preferenceCategory.addPreference(identityPref)
        }

        identityPref.apply {
            key = uniqueIdentityPrefKey
            isPersistent = false
            title = identity.email
            isChecked = identity.openPgpEnabled
            summaryOn = getString(getIdentitySummaryResId(identity))
            summaryOff = getString(R.string.openpgp_settings_mode_disabled)

            fragment = OpenPgpSettingsIdentityFragment::class.java.name
            extras.apply {
                putString(OpenPgpSettingsIdentityFragment.ARGUMENT_ACCOUNT_UUID, account.uuid)
                putInt(OpenPgpSettingsIdentityFragment.ARGUMENT_IDENTITY_INDEX, identityIndex)
                putParcelable(OpenPgpSettingsIdentityFragment.ARGUMENT_IDENTITY, identity)
            }

            setOnPreferenceChangeListener { preference, newValue ->
                enableDisablePgpOnIdentity(preference, newValue as Boolean)
                true
            }
        }
    }

    private fun getIdentitySummaryResId(identity: Identity): Int {
        return when {
            identity.openPgpModeMutual -> R.string.openpgp_settings_mode_automatic
            else -> R.string.openpgp_settings_mode_manual
        }
    }

    private fun enableDisablePgpOnIdentity(preference: Preference, checked: Boolean) {
        val account = preferences.getAccount(preference.extras.getString(OpenPgpSettingsIdentityFragment.ARGUMENT_ACCOUNT_UUID))
        val identityIndex = preference.extras.getInt(OpenPgpSettingsIdentityFragment.ARGUMENT_IDENTITY_INDEX, -1)
        val identity = preference.extras.getParcelable<Identity>(OpenPgpSettingsIdentityFragment.ARGUMENT_IDENTITY)!!

        val newIdentity = identity.copy(openPgpEnabled = checked)
        preferences.saveIdentity(account, identityIndex, newIdentity)
    }

    override fun onCreateRecyclerView(inflater: LayoutInflater?, parent: ViewGroup?, savedInstanceState: Bundle?): RecyclerView {
        // Disable animations when adding the identity preferences
        val view = super.onCreateRecyclerView(inflater, parent, savedInstanceState)
        view.itemAnimator = null
        view.layoutAnimation = null
        return view
    }

    companion object {
        const val KEY_CATEGORY_IDENTITIES = "openpgp_identities"
        const val KEY_MANAGE = "openpgp_manage"

        fun create(rootKey: String? = null) = OpenPgpSettingsFragment().withArguments(ARG_PREFERENCE_ROOT to rootKey)
    }
}
