package app.k9mail.feature.migration.qrcode.domain.usecase

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.multi.qrcode.QRCodeMultiReader
import timber.log.Timber

/**
 * An [ImageAnalysis.Analyzer] that scans for QR codes and notifies the listener for each one found.
 */
internal class QrCodeAnalyzer(
    private val qrCodeListener: (String) -> Unit,
) : ImageAnalysis.Analyzer {
    private val qrCodeReader = QRCodeMultiReader()

    override fun analyze(image: ImageProxy) {
        val plane = image.planes[0]
        val buffer = plane.buffer
        val data = ByteArray(buffer.remaining()).also { buffer.get(it) }

        val height = image.height
        val width = image.width
        val dataWidth = width + ((plane.rowStride - plane.pixelStride * width) / plane.pixelStride)
        val luminanceSource = PlanarYUVLuminanceSource(data, dataWidth, height, 0, 0, width, height, false)

        val results = decodeSource(luminanceSource)
        for (result in results) {
            qrCodeListener(result)
        }

        image.close()
    }

    @Suppress("TooGenericExceptionCaught")
    private fun decodeSource(source: LuminanceSource): List<String> {
        return try {
            val bitmap = createBinaryBitmap(source)
            val results = qrCodeReader.decodeMultiple(bitmap, DECODER_HINTS)

            results.map { it.text }
        } catch (e: Exception) {
            Timber.e(e, "Error while trying to read QR code")
            emptyList()
        } finally {
            qrCodeReader.reset()
        }
    }

    private fun createBinaryBitmap(source: LuminanceSource): BinaryBitmap {
        return BinaryBitmap(HybridBinarizer(source))
    }

    companion object {
        private val DECODER_HINTS = mapOf(
            DecodeHintType.CHARACTER_SET to "UTF-8",
            DecodeHintType.TRY_HARDER to true,
        )
    }
}
