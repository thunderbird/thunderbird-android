package net.thunderbird.feature.mail.message.composer.dialog

import androidx.fragment.app.FragmentManager

interface SmimeSigningDialogFragmentFactory {
    companion object {
        const val REQUEST_KEY_SMIME_SIGNING_RESULT = "SmimeSigningDialogFragmentFactory_result"
        const val RESULT_ACTION = "SmimeSigningDialogFragmentFactory_action"
        const val RESULT_CERTIFICATE_ALIAS = "SmimeSigningDialogFragmentFactory_certificateAlias"
        const val RESULT_SET_AS_DEFAULT = "SmimeSigningDialogFragmentFactory_setAsDefault"

        const val ACTION_SIGN = "sign"
        const val ACTION_SEND_UNSIGNED = "send_unsigned"
        const val ACTION_DISABLE_SIGNING = "disable_signing"
    }

    fun show(
        fragmentManager: FragmentManager,
        currentCertificateAlias: String?,
        isDefaultForAccount: Boolean,
        isSigningEnabledForMessage: Boolean,
        isFirstTimeSendPrompt: Boolean,
    )
}
