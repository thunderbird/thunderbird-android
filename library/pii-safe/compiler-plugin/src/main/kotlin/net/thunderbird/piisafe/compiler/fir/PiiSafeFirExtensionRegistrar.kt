package net.thunderbird.piisafe.compiler.fir

import net.thunderbird.piisafe.compiler.fir.checkers.PiiSafeFirCheckers
import net.thunderbird.piisafe.compiler.fir.generation.ToStringOverridePiiSafeDeclarationGenerator
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

internal class PiiSafeFirExtensionRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +::PiiSafeFirCheckers
        +::ToStringOverridePiiSafeDeclarationGenerator
    }
}
