package net.thunderbird.piisafe.compiler.ir.transformer

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.builders.Scope

/**
 * Abstract base class for IR transformers that require access to both plugin context and scope information.
 *
 * Extends IrElementTransformerVoidWithContext to provide a convenient way to access the current scope
 * and plugin context during IR tree transformation. Subclasses must provide an IrPluginContext instance
 * and can access the current scope through the scope property.
 */
internal abstract class IrScopedTransformer : IrElementTransformerVoidWithContext() {
    /**
     * Provides access to the IR plugin context for symbol resolution and IR element creation.
     *
     * This context is required for accessing built-in types, creating IR elements, and resolving
     * symbols during the IR transformation process.
     */
    internal abstract val pluginContext: IrPluginContext

    /**
     * Returns the current scope during IR transformation, or null if no scope is active.
     *
     * This property provides access to the scope information from the parent IrElementTransformerVoidWithContext,
     * allowing subclasses to obtain the current scope owner symbol for IR element creation and transformation.
     */
    internal val scope: Scope? get() = currentScope?.scope
}
