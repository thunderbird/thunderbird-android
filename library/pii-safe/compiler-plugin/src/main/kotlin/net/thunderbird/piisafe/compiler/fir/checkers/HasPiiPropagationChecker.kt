package net.thunderbird.piisafe.compiler.fir.checkers

import net.thunderbird.piisafe.compiler.extension.fir.isAnnotatedWithHasPii
import net.thunderbird.piisafe.compiler.fir.checkers.errors.PiiSafeFirErrors
import net.thunderbird.piisafe.compiler.symbols.ProjectFqNames
import net.thunderbird.piisafe.compiler.symbols.asClassId
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirDeclarationChecker
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.processAllDeclarations
import org.jetbrains.kotlin.fir.declarations.utils.isData
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol

internal class HasPiiPropagationChecker(
    private val session: FirSession,
) : FirDeclarationChecker<FirClass>(mppKind = MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirClass) {
        val classId = ProjectFqNames.PiiSafeHasPiiFqName.asClassId(ProjectFqNames.PiiSafeAnnotationsPackageFqName)
        if (declaration.symbol.isData && !declaration.isAnnotatedWithHasPii(session)) {
            declaration.processAllDeclarations(session) { symbol ->
                when (symbol) {
                    is FirPropertySymbol -> {
                        val typeClassSymbol = symbol.resolvedReturnType.toClassSymbol(session)
                        if (typeClassSymbol?.hasAnnotation(classId, session) == true) {
                            reporter.reportOn(
                                source = symbol.source,
                                factory = PiiSafeFirErrors.HAS_PII_TYPE_ESCAPES_UNANNOTATED_CLASS,
                            )
                        }
                    }
                }
            }
        }
    }
}
