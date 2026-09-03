package net.thunderbird.piisafe.compiler.fir.checkers.errors

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.error0
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.psi.KtElement

internal object PiiLoggingFirErrors : KtDiagnosticsContainer() {
    val HAS_PII_CLASS_WITHOUT_ANNOTATED_PROPERTY by error0<KtElement>()
    val HAS_PII_TYPE_ESCAPES_UNANNOTATED_CLASS by error0<KtElement>()

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = PiiLoggingFirErrorsRenderer
}

private object PiiLoggingFirErrorsRenderer : BaseDiagnosticRendererFactory() {
    override val MAP: KtDiagnosticFactoryToRendererMap by KtDiagnosticFactoryToRendererMap(name = "PIILogging") { map ->
        map.put(
            PiiLoggingFirErrors.HAS_PII_CLASS_WITHOUT_ANNOTATED_PROPERTY,
            "Class annotated with @LoggingPii.HasPii must have at least one property annotated with " +
                "@LoggingPii.Ignore or @LoggingPii.Mask.",
        )
        map.put(
            PiiLoggingFirErrors.HAS_PII_TYPE_ESCAPES_UNANNOTATED_CLASS,
            "This class holds a property of a type annotated @LoggingPii.HasPii; this class must also " +
                "be annotated @LoggingPii.HasPii.",
        )
    }
}
