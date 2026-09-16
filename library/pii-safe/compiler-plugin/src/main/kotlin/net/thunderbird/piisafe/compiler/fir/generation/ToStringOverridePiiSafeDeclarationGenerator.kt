package net.thunderbird.piisafe.compiler.fir.generation

import net.thunderbird.piisafe.compiler.PiiSafePluginKey
import net.thunderbird.piisafe.compiler.extension.fir.isAnnotatedWithHasPii
import net.thunderbird.piisafe.compiler.symbols.ProjectFqNames
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.utils.isData
import org.jetbrains.kotlin.fir.extensions.DeclarationGenerationContext
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.plugin.createMemberFunction
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.name.CallableId
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
    ): Set<Name> = if (classSymbol.isAnnotatedWithHasPii(session) && classSymbol.isData) {
        setOf(TO_STRING_NAME)
    } else {
        emptySet()
    }

    override fun generateFunctions(
        callableId: CallableId,
        context: MemberGenerationContext?,
    ): List<FirNamedFunctionSymbol> {
        val owner = context?.owner
        if (callableId.callableName != TO_STRING_NAME || owner == null) return emptyList()

        val function = createMemberFunction(
            owner = owner,
            key = PiiSafePluginKey,
            name = TO_STRING_NAME,
            returnType = session.builtinTypes.stringType.coneType,
        ) {
            modality = Modality.FINAL
            status { isOverride = true }
            withGeneratedDefaultBody()
        }
        return listOf(function.symbol)
    }

    internal companion object {
        const val TO_STRING_METHOD_NAME = "toString"
        val TO_STRING_NAME = Name.identifier(TO_STRING_METHOD_NAME)
    }
}
