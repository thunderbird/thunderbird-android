package net.thunderbird.piisafe.compiler.fir.checkers

import net.thunderbird.piisafe.compiler.extension.fir.isAnnotatedWithHasPii
import net.thunderbird.piisafe.compiler.fir.checkers.errors.PiiSafeFirErrors
import net.thunderbird.piisafe.compiler.symbols.ProjectClassIds
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.processAllDeclarations
import org.jetbrains.kotlin.fir.declarations.utils.isData
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol

internal class HasPiiRequiresAnnotatedPropertyChecker(
    private val session: FirSession,
) : FirClassChecker(mppKind = MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirClass) {
        if (declaration.symbol.isData && declaration.isAnnotatedWithHasPii(session)) {
            var anyPropertyAnnotated = false
            declaration.processAllDeclarations(session) { symbol ->
                when (symbol) {
                    is FirPropertySymbol if (
                        symbol.isAnnotatedWithPiiSafeMask(session) ||
                            symbol.isAnnotatedWithPiiSafeIgnore(session) ||
                            symbol.isOfHasPiiAnnotatedType(session)
                        ) -> {
                        anyPropertyAnnotated = true
                        return@processAllDeclarations
                    }
                }
            }

            if (!anyPropertyAnnotated) {
                reporter.reportOn(declaration.source, PiiSafeFirErrors.HAS_PII_CLASS_WITHOUT_ANNOTATED_PROPERTY)
            }
        }
    }

    private fun FirPropertySymbol.isAnnotatedWithPiiSafeMask(session: FirSession): Boolean =
        getterSymbol?.hasAnnotation(ProjectClassIds.PiiSafeMaskClassId, session) == true

    private fun FirPropertySymbol.isAnnotatedWithPiiSafeIgnore(session: FirSession): Boolean =
        getterSymbol?.hasAnnotation(ProjectClassIds.PiiSafeHideClassId, session) == true

    private fun FirPropertySymbol.isOfHasPiiAnnotatedType(session: FirSession): Boolean =
        resolvedReturnType.toClassSymbol(session)?.isAnnotatedWithHasPii(session) == true
}
