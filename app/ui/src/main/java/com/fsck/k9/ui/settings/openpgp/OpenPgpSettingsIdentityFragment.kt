package com.fsck.k9.ui.settings.openpgp


import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.preference.Preference
import androidx.recyclerview.widget.RecyclerView
import com.fsck.k9.Account
import com.fsck.k9.Identity
import com.fsck.k9.Preferences
import com.fsck.k9.ui.R
import com.fsck.k9.ui.settings.account.AutocryptPreferEncryptPreference
import com.takisoft.preferencex.PreferenceFragmentCompatMasterSwitch
import com.takisoft.preferencex.PreferenceFragmentCompatMasterSwitch.OnMasterSwitchChangeListener
import org.koin.android.ext.android.inject

class OpenPgpSettingsIdentityFragment : PreferenceFragmentCompatMasterSwitch() {
    private val preferences: Preferences by inject()

    private lateinit var account: Account
    private lateinit var identity: Identity
    private var identityIndex: Int = -1

    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.openpgp_identity_settings, rootKey)

        val accountUuid = arguments!!.getString(ARGUMENT_ACCOUNT_UUID)!!
        account = preferences.getAccount(accountUuid)
        identityIndex = arguments!!.getInt(ARGUMENT_IDENTITY_INDEX, -1)
        identity = arguments!!.getParcelable(ARGUMENT_IDENTITY)!!

        masterSwitch.isChecked = identity.openPgpEnabled

        masterSwitch.onPreferenceChangeListener = OnMasterSwitchChangeListener { newValue ->
            enableDisableOpenPgp(newValue)
            true
        }

        val openPgpFingerprint = findPreference<Preference>(KEY_FINGERPRINT)!!
        openPgpFingerprint.summary = identity.openPgpKey.toString()

        val openPgpMutualMode = findPreference<AutocryptPreferEncryptPreference>(KEY_MUTUAL_MODE)!!
        openPgpMutualMode.isChecked = identity.openPgpModeMutual
        openPgpMutualMode.setOnPreferenceChangeListener { _, newValue ->
            enableDisableMutualMode(newValue as Boolean)
            true
        }

        updateInfoViewState(masterSwitch.isChecked)
    }

    private fun enableDisableMutualMode(checked: Boolean) {
        identity = identity.copy(openPgpModeMutual = checked)
        preferences.saveIdentity(account, identityIndex, identity)
    }

    private fun enableDisableOpenPgp(checked: Boolean) {
        updateInfoViewState(checked)

        identity = identity.copy(openPgpEnabled = checked)
        preferences.saveIdentity(account, identityIndex, identity)
    }

    private fun updateInfoViewState(checked: Boolean) {
        findPreference<Preference>(KEY_CAT_ENABLED)!!.isVisible = checked
        findPreference<Preference>(KEY_CAT_DISABLED)!!.isVisible = !checked
    }

    override fun onCreateRecyclerView(inflater: LayoutInflater?, parent: ViewGroup?, savedInstanceState: Bundle?): RecyclerView {
        // Disable animations here to prevent flashing when master switch is enabled/disabled
        val view = super.onCreateRecyclerView(inflater, parent, savedInstanceState)
        view.itemAnimator = null
        view.layoutAnimation = null
        return view
    }

    companion object {
        const val KEY_CAT_ENABLED = "cat_enabled"
        const val KEY_CAT_DISABLED = "cat_disabled"
        const val KEY_FINGERPRINT = "openpgp_fingerprint"
        const val KEY_MUTUAL_MODE = "openpgp_mutual_mode"

        const val ARGUMENT_IDENTITY = "identity"
        const val ARGUMENT_ACCOUNT_UUID = "account_uuid"
        const val ARGUMENT_IDENTITY_INDEX = "identity_index"
    }
}