package com.fsck.k9.ui.settings.account

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import com.ciphermail.smime.api.util.SmimeApi
import com.ciphermail.smime.api.util.SmimeServiceConnection
import com.fsck.k9.Preferences
import com.fsck.k9.ui.R
import com.fsck.k9.ui.base.BaseActivity
import com.fsck.k9.ui.base.ThemeType
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.logging.legacy.Log

/**
 * Provider-picker dialog hosted by an activity.
 *
 * Enumerates every package on the device that exposes an [com.ciphermail.smime.api.ISmimeService]
 * (matching the [SmimeApi.SERVICE_INTENT] action). The user's selection is persisted on the account
 * as [LegacyAccountDto.smimeProvider]. From then on, [SmimeServiceConnection] binds with an
 * explicit `setPackage()` so we never re-resolve by intent alone — a malicious app cannot
 * intercept the binding by declaring a higher priority filter.
 *
 * Launched from `AccountSettingsFragment`'s S/MIME preference row when the user enables S/MIME
 * support. If no providers are installed, the row shows
 * `R.string.account_settings_smime_no_provider_title` instead of opening this picker.
 */
class SmimeAppSelectDialog : BaseActivity(ThemeType.DIALOG) {
    private lateinit var account: LegacyAccountDto

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val accountUuid = requireNotNull(intent.getStringExtra(EXTRA_ACCOUNT)) {
            "missing $EXTRA_ACCOUNT extra"
        }
        account = requireNotNull(Preferences.getPreferences().getAccount(accountUuid)) {
            "no such account: $accountUuid"
        }
    }

    override fun onStart() {
        super.onStart()
        val smimeProviderPackages = getSmimeProviderPackages()
        when {
            smimeProviderPackages.isEmpty() -> finish()
            smimeProviderPackages.size == 1 -> {
                Log.d("Only one S/MIME provider - just choosing that one!")
                persistSmimeProviderSetting(smimeProviderPackages[0])
                finish()
            }
            else -> showSmimeSelectDialogFragment()
        }
    }

    private fun getSmimeProviderPackages(): List<String> {
        val intent = Intent(SmimeApi.SERVICE_INTENT)
        return packageManager.queryIntentServices(intent, 0)
            .mapNotNull { it.serviceInfo?.packageName }
    }

    private fun showSmimeSelectDialogFragment() {
        SmimeAppSelectFragment().show(supportFragmentManager, FRAG_SMIME_SELECT)
    }

    fun onSelectProvider(selectedPackage: String?) {
        if (selectedPackage != null) {
            persistSmimeProviderSetting(selectedPackage)
        }
        finish()
    }

    private fun persistSmimeProviderSetting(selectedPackage: String) {
        account.smimeProvider = selectedPackage
        account.smimeEnabled = true
        Preferences.getPreferences().saveAccount(account)
    }

    class SmimeAppSelectFragment : DialogFragment() {
        private val smimeProviderList = mutableListOf<SmimeProviderEntry>()
        private var selectedPackage: String? = null

        private fun populateAppList() {
            smimeProviderList.clear()
            val context = requireActivity()
            val intent = Intent(SmimeApi.SERVICE_INTENT)
            context.packageManager.queryIntentServices(intent, 0).forEach { resolveInfo ->
                val serviceInfo = resolveInfo.serviceInfo ?: return@forEach
                smimeProviderList.add(
                    SmimeProviderEntry(
                        packageName = serviceInfo.packageName,
                        simpleName = serviceInfo.loadLabel(context.packageManager).toString(),
                        icon = serviceInfo.loadIcon(context.packageManager),
                    ),
                )
            }
        }

        override fun onStop() {
            super.onStop()
            dismissAllowingStateLoss()
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            populateAppList()
            val activity = requireActivity()
            val adapter = object : ArrayAdapter<SmimeProviderEntry>(
                activity,
                R.layout.select_openpgp_app_item,
                android.R.id.text1,
                smimeProviderList,
            ) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val v = super.getView(position, convertView, parent)
                    v.findViewById<ImageView>(android.R.id.icon1)
                        .setImageDrawable(smimeProviderList[position].icon)
                    return v
                }
            }

            return MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.account_settings_smime_app_select_title)
                .setSingleChoiceItems(adapter, -1) { dialog, which ->
                    selectedPackage = smimeProviderList[which].packageName
                    dialog.dismiss()
                }
                .create()
        }

        override fun onDismiss(dialog: DialogInterface) {
            super.onDismiss(dialog)
            (activity as? SmimeAppSelectDialog)?.onSelectProvider(selectedPackage)
        }
    }

    private data class SmimeProviderEntry(
        val packageName: String,
        val simpleName: String,
        val icon: Drawable,
    ) {
        override fun toString(): String = simpleName
    }

    companion object {
        private const val EXTRA_ACCOUNT = "account"
        const val FRAG_SMIME_SELECT = "smime_select"

        /** Launch the picker for the given account. */
        @JvmStatic
        fun startSmimeChooserActivity(context: Context, account: LegacyAccountDto) {
            val intent = Intent(context, SmimeAppSelectDialog::class.java).apply {
                putExtra(EXTRA_ACCOUNT, account.uuid)
            }
            context.startActivity(intent)
        }
    }
}
