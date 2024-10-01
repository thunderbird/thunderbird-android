package app.k9mail.feature.migration.qrcode.settings

import androidx.core.content.FileProvider

/**
 * Used to exposes account information read from QR codes via a content URI to the settings import code.
 */
class QrCodeSettingsFileProvider : FileProvider()
