package net.thunderbird.piisafe.compiler.fir

import net.thunderbird.piisafe.compiler.fir.checkers.PiiLoggingFirCheckers
import net.thunderbird.piisafe.compiler.fir.generation.ToStringOverridePiiSafeDeclarationGenerator
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

internal class PiiLoggingFirExtensionRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +::PiiLoggingFirCheckers
        +::ToStringOverridePiiSafeDeclarationGenerator
    }
}
