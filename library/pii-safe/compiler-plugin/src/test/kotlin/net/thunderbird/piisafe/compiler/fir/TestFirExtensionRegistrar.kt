package net.thunderbird.piisafe.compiler.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

/**
 * A [FirExtensionRegistrar] that registers only the FIR extensions a test explicitly asks for, so a single
 * extension can be exercised in isolation instead of running the whole plugin.
 *
 * @param checkersFactory Factories of the [FirAdditionalCheckersExtension]s to register.
 * @param declarationGenerationFactory Factories of the [FirDeclarationGenerationExtension]s to register.
 */
internal class TestFirExtensionRegistrar(
    private val checkersFactory: List<(FirSession) -> FirAdditionalCheckersExtension> = emptyList(),
    private val declarationGenerationFactory: List<(FirSession) -> FirDeclarationGenerationExtension> = emptyList(),
) : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        checkersFactory.forEach { +it }
        declarationGenerationFactory.forEach { +it }
    }
}

/**
 * Creates a [TestFirExtensionRegistrar] that registers only the given [FirAdditionalCheckersExtension] factories.
 *
 * @param checkersFactory Factories of the checkers extensions to register.
 * @return A registrar exposing the given checkers extensions and no declaration generators.
 */
@JvmName("TestFirExtensionRegistrarWithCheckers")
internal fun TestFirExtensionRegistrar(
    vararg checkersFactory: (FirSession) -> FirAdditionalCheckersExtension,
): TestFirExtensionRegistrar = TestFirExtensionRegistrar(checkersFactory = checkersFactory.toList())

/**
 * Creates a [TestFirExtensionRegistrar] that registers only the given [FirDeclarationGenerationExtension] factories.
 *
 * @param declarationGenerationFactory Factories of the declaration generation extensions to register.
 * @return A registrar exposing the given declaration generation extensions and no checkers.
 */
@JvmName("TestFirExtensionRegistrarWithDeclarationGenerationsFactory")
internal fun TestFirExtensionRegistrar(
    vararg declarationGenerationFactory: (FirSession) -> FirDeclarationGenerationExtension,
): TestFirExtensionRegistrar = TestFirExtensionRegistrar(
    declarationGenerationFactory = declarationGenerationFactory.toList(),
)
