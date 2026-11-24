package net.thunderbird.feature.mail.message.composer.internal.dialog

import android.os.Bundle
import android.security.KeyChain
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import java.util.concurrent.Executors
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider
import net.thunderbird.feature.mail.message.composer.dialog.SmimeSigningDialogFragmentFactory
import net.thunderbird.feature.mail.message.composer.dialog.SmimeSigningDialogFragmentFactory.Companion.ACTION_DISABLE_SIGNING
import net.thunderbird.feature.mail.message.composer.dialog.SmimeSigningDialogFragmentFactory.Companion.ACTION_SEND_UNSIGNED
import net.thunderbird.feature.mail.message.composer.dialog.SmimeSigningDialogFragmentFactory.Companion.ACTION_SIGN
import net.thunderbird.feature.mail.message.composer.dialog.SmimeSigningDialogFragmentFactory.Companion.REQUEST_KEY_SMIME_SIGNING_RESULT
import net.thunderbird.feature.mail.message.composer.dialog.SmimeSigningDialogFragmentFactory.Companion.RESULT_ACTION
import net.thunderbird.feature.mail.message.composer.dialog.SmimeSigningDialogFragmentFactory.Companion.RESULT_CERTIFICATE_ALIAS
import net.thunderbird.feature.mail.message.composer.dialog.SmimeSigningDialogFragmentFactory.Companion.RESULT_SET_AS_DEFAULT
import net.thunderbird.feature.mail.message.composer.smime.SmimeCertificateInfo
import net.thunderbird.feature.mail.message.composer.smime.SmimeKeyChainProvider
import org.koin.android.ext.android.inject

internal class SmimeSigningDialogFragment : DialogFragment() {
    private val themeProvider: FeatureThemeProvider by inject()
    private val keyChainProvider: SmimeKeyChainProvider by inject()
    private val backgroundExecutor = Executors.newSingleThreadExecutor()

    @Suppress("LongMethod")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val args = requireArguments()
        val initialAlias = args.getString(ARG_CURRENT_ALIAS)
        val initialDefault = args.getBoolean(ARG_IS_DEFAULT_FOR_ACCOUNT, false)
        val isSigningEnabledForMessage = args.getBoolean(ARG_IS_SIGNING_ENABLED_FOR_MESSAGE, false)
        val isFirstTimeSendPrompt = args.getBoolean(ARG_IS_FIRST_TIME_SEND_PROMPT, false)

        dialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)

        return ComposeView(requireContext()).apply {
            setContent {
                themeProvider.WithTheme {
                    var selectedAlias by rememberSaveable { mutableStateOf(initialAlias) }
                    var setAsDefault by rememberSaveable { mutableStateOf(initialDefault) }
                    var showNoCertificateHint by rememberSaveable { mutableStateOf(false) }
                    var certificateInfo by remember { mutableStateOf<SmimeCertificateInfo?>(null) }

                    LaunchedEffect(selectedAlias) {
                        val alias = selectedAlias
                        if (alias.isNullOrBlank()) {
                            certificateInfo = null
                        } else {
                            loadCertificateInfoAsync(alias) { info ->
                                if (selectedAlias == alias) {
                                    certificateInfo = info
                                }
                            }
                        }
                    }

                    SmimeSigningDialog(
                        selectedAlias = selectedAlias,
                        certificateInfo = certificateInfo,
                        setAsDefault = setAsDefault,
                        isSigningEnabledForMessage = isSigningEnabledForMessage,
                        isFirstTimeSendPrompt = isFirstTimeSendPrompt,
                        showNoCertificateHint = showNoCertificateHint,
                        onChooseCertificateClick = {
                            launchCertificatePicker(preselectedAlias = selectedAlias) { chosenAlias ->
                                if (chosenAlias != null) {
                                    selectedAlias = chosenAlias
                                    showNoCertificateHint = false
                                } else {
                                    showNoCertificateHint = true
                                }
                            }
                        },
                        onSetAsDefaultChange = { checked -> setAsDefault = checked },
                        onConfirmSignClick = {
                            val alias = selectedAlias ?: return@SmimeSigningDialog
                            deliverResult(
                                action = ACTION_SIGN,
                                certificateAlias = alias,
                                setAsDefault = setAsDefault,
                            )
                        },
                        onSendUnsignedClick = {
                            deliverResult(
                                action = ACTION_SEND_UNSIGNED,
                                certificateAlias = null,
                                setAsDefault = false,
                            )
                        },
                        onDisableSigningClick = {
                            deliverResult(
                                action = ACTION_DISABLE_SIGNING,
                                certificateAlias = selectedAlias,
                                setAsDefault = false,
                            )
                        },
                        onDismiss = ::dismiss,
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        backgroundExecutor.shutdownNow()
        super.onDestroy()
    }

    private fun launchCertificatePicker(
        preselectedAlias: String?,
        onAliasSelected: (String?) -> Unit,
    ) {
        val hostActivity = activity ?: return
        KeyChain.choosePrivateKeyAlias(
            hostActivity,
            { alias ->
                hostActivity.runOnUiThread {
                    if (isAdded) {
                        onAliasSelected(alias)
                    }
                }
            },
            null,
            null,
            null,
            -1,
            preselectedAlias,
        )
    }

    private fun loadCertificateInfoAsync(
        alias: String,
        onLoaded: (SmimeCertificateInfo?) -> Unit,
    ) {
        backgroundExecutor.execute {
            val info = keyChainProvider.getCertificateInfo(alias)
            activity?.runOnUiThread {
                if (isAdded) {
                    onLoaded(info)
                }
            }
        }
    }

    private fun deliverResult(
        action: String,
        certificateAlias: String?,
        setAsDefault: Boolean,
    ) {
        dismiss()
        setFragmentResult(
            requestKey = REQUEST_KEY_SMIME_SIGNING_RESULT,
            result = Bundle().apply {
                putString(RESULT_ACTION, action)
                putString(RESULT_CERTIFICATE_ALIAS, certificateAlias)
                putBoolean(RESULT_SET_AS_DEFAULT, setAsDefault)
            },
        )
    }

    companion object Factory : SmimeSigningDialogFragmentFactory {
        private const val TAG = "SmimeSigningDialogFragment"
        private const val ARG_CURRENT_ALIAS = "currentCertificateAlias"
        private const val ARG_IS_DEFAULT_FOR_ACCOUNT = "isDefaultForAccount"
        private const val ARG_IS_SIGNING_ENABLED_FOR_MESSAGE = "isSigningEnabledForMessage"
        private const val ARG_IS_FIRST_TIME_SEND_PROMPT = "isFirstTimeSendPrompt"

        override fun show(
            fragmentManager: FragmentManager,
            currentCertificateAlias: String?,
            isDefaultForAccount: Boolean,
            isSigningEnabledForMessage: Boolean,
            isFirstTimeSendPrompt: Boolean,
        ) {
            if (fragmentManager.findFragmentByTag(TAG) != null) {
                return
            }
            SmimeSigningDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CURRENT_ALIAS, currentCertificateAlias)
                    putBoolean(ARG_IS_DEFAULT_FOR_ACCOUNT, isDefaultForAccount)
                    putBoolean(ARG_IS_SIGNING_ENABLED_FOR_MESSAGE, isSigningEnabledForMessage)
                    putBoolean(ARG_IS_FIRST_TIME_SEND_PROMPT, isFirstTimeSendPrompt)
                }
                show(fragmentManager, TAG)
            }
        }
    }
}
