package net.thunderbird.piisafe.compiler.fir.generation

import net.thunderbird.piisafe.compiler.symbols.ProjectFqNames
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.DeclarationGenerationContext
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.name.Name

internal class ToStringOverridePiiSafeDeclarationGenerator(session: FirSession) :
    FirDeclarationGenerationExtension(session) {
    private val hasPiiPredicate = LookupPredicate.create {
        annotated(ProjectFqNames.PiiSafeHasPiiFqName)
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(hasPiiPredicate)
    }

    override fun getCallableNamesForClass(
        classSymbol: FirClassSymbol<*>,
        context: DeclarationGenerationContext.Member,
    ): Set<Name> = emptySet()
}
