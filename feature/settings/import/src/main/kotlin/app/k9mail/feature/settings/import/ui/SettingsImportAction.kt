package app.k9mail.feature.settings.import.ui

import android.os.Bundle

enum class SettingsImportAction(private val bundleValue: String) {
    Overview("overview"),
    ScanQrCode("scan_qr_code"),
    ;

    fun toBundle(): Bundle {
        return Bundle().apply {
            putString(BUNDLE_KEY_ACTION, bundleValue)
        }
    }

    companion object {
        const val BUNDLE_KEY_ACTION = "action"

        fun fromBundle(bundle: Bundle): SettingsImportAction {
            val actionString = bundle.getString(BUNDLE_KEY_ACTION)
            return entries.find { it.bundleValue == actionString } ?: Overview
        }
    }
}
