package app.k9mail.feature.migration.qrcode.domain.usecase

import android.util.Size
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.UseCase
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import app.k9mail.feature.migration.qrcode.domain.QrCodeDomainContract.UseCase.CameraUseCasesProvider
import java.util.concurrent.Executors

/**
 * Returns a CameraX [ImageAnalysis] instance that will scan for QR codes and notify the provided listener.
 */
internal class QrCodeImageAnalysisProvider(
    private val qrCodeListener: (String) -> Unit,
) : CameraUseCasesProvider {
    override fun getUseCases(): List<UseCase> {
        val qrCodeAnalyzer = QrCodeAnalyzer(qrCodeListener)

        val resolutionSelector = ResolutionSelector.Builder()
            .setResolutionStrategy(
                ResolutionStrategy(
                    Size(TARGET_WIDTH, TARGET_HEIGHT),
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                ),
            )
            .build()

        val qrCodeImageAnalysis = ImageAnalysis.Builder()
            .setImageQueueDepth(1)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setResolutionSelector(resolutionSelector)
            .build()
            .apply {
                setAnalyzer(Executors.newSingleThreadExecutor(), qrCodeAnalyzer)
            }

        return listOf(qrCodeImageAnalysis)
    }

    companion object {
        private const val TARGET_WIDTH = 1920
        private const val TARGET_HEIGHT = 1080
    }
}
