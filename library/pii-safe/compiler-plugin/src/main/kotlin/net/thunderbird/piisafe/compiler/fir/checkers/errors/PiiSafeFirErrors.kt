package net.thunderbird.piisafe.compiler.fir.checkers.errors

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.error0
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.psi.KtElement

internal object PiiSafeFirErrors : KtDiagnosticsContainer() {
    val HAS_PII_CLASS_WITHOUT_ANNOTATED_PROPERTY by error0<KtElement>()
    val HAS_PII_TYPE_ESCAPES_UNANNOTATED_CLASS by error0<KtElement>()

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = PiiSafeFirErrorsRenderer
}

private object PiiSafeFirErrorsRenderer : BaseDiagnosticRendererFactory() {
    override val MAP: KtDiagnosticFactoryToRendererMap by KtDiagnosticFactoryToRendererMap(name = "PII-safe") { map ->
        map.put(
            factory = PiiSafeFirErrors.HAS_PII_CLASS_WITHOUT_ANNOTATED_PROPERTY,
            message = "Class annotated with @PiiSafe.HasPii must have at least one property annotated with " +
                "@PiiSafe.Hide or @PiiSafe.Mask.",
        )
        map.put(
            factory = PiiSafeFirErrors.HAS_PII_TYPE_ESCAPES_UNANNOTATED_CLASS,
            message = "This class holds a property of a type annotated @PiiSafe.HasPii; this class must also " +
                "be annotated @PiiSafe.HasPii.",
        )
    }
}
