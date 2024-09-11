package app.k9mail.feature.migration.qrcode

import com.squareup.moshi.JsonDataException
import java.io.IOException
import timber.log.Timber

internal class QrCodePayloadParser(
    private val qrCodePayloadAdapter: QrCodePayloadAdapter = QrCodePayloadAdapter(),
) {
    /**
     * Parses the QR code payload as JSON and reads it into [QrCodeData].
     *
     * @return [QrCodeData] if the JSON was parsed successfully and has the correct structure, `null` otherwise.
     */
    fun parse(payload: String): QrCodeData? {
        return try {
            qrCodePayloadAdapter.fromJson(payload)
        } catch (e: JsonDataException) {
            Timber.d(e, "Failed to parse JSON")
            null
        } catch (e: IOException) {
            Timber.d(e, "Unexpected IOException")
            null
        }
    }
}
