package com.fsck.k9.ui.onboarding

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.fsck.k9.Preferences
import com.fsck.k9.activity.MessageList
import com.fsck.k9.search.LocalSearch
import com.fsck.k9.ui.R
import com.fsck.k9.ui.helper.HtmlToSpanned
import com.fsck.k9.ui.observeNotNull
import com.fsck.k9.ui.settings.import.SettingsImportResultViewModel
import com.fsck.k9.ui.settings.import.SettingsImportSuccess
import org.koin.android.architecture.ext.sharedViewModel
import org.koin.android.ext.android.inject

class WelcomeFragment : Fragment() {
    private val htmlToSpanned: HtmlToSpanned by inject()
    private val preferences: Preferences by inject()
    private val importResultViewModel: SettingsImportResultViewModel by sharedViewModel()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_welcome_message, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val welcome: TextView = view.findViewById(R.id.welcome_message)
        welcome.text = htmlToSpanned.convert(getString(R.string.accounts_welcome))
        welcome.movementMethod = LinkMovementMethod.getInstance()

        view.findViewById<View>(R.id.next).setOnClickListener { launchAccountSetup() }
        view.findViewById<View>(R.id.import_settings).setOnClickListener { launchImportSettings() }

        importResultViewModel.settingsImportResult.observeNotNull(this) {
            if (it == SettingsImportSuccess) {
                launchMessageList()
            }
        }
    }

    private fun launchAccountSetup() {
        findNavController().navigate(R.id.action_welcomeScreen_to_addAccountScreen)
        requireActivity().finish()
    }

    private fun launchImportSettings() {
        findNavController().navigate(R.id.action_welcomeScreen_to_settingsImportScreen)
    }

    private fun launchMessageList() {
        //TODO: Remove this and use NavController to navigate to MessageList once GH-4230 has been implemented
        val account = preferences.defaultAccount
        val folder = account.autoExpandFolder ?: account.inboxFolder
        val search = LocalSearch(folder)
        search.addAllowedFolder(folder)
        search.addAccountUuid(account.uuid)
        MessageList.actionDisplaySearch(requireActivity(), search, false, true)

        requireActivity().finish()
    }
}
