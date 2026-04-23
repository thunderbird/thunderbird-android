package app.k9mail.feature.settings.import.ui

import android.os.Bundle
import androidx.core.os.bundleOf

enum class SettingsImportAction(private val bundleValue: String) {
    Overview("overview"),
    ScanQrCode("scan_qr_code"),
    PickDocument("pick_document"),
    PickApp("pick_app"),
    ;

    fun toBundle(): Bundle {
        return bundleOf(
            BUNDLE_KEY_ACTION to bundleValue,
        )
    }

    companion object {
        const val BUNDLE_KEY_ACTION = "action"

        fun fromBundle(bundle: Bundle): SettingsImportAction {
            val actionString = bundle.getString(BUNDLE_KEY_ACTION)
            return entries.find { it.bundleValue == actionString } ?: Overview
        }
    }
}
