package net.thunderbird.piisafe.compiler.ir.builder

import net.thunderbird.piisafe.compiler.ir.transformer.IrScopedTransformer
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI

/**
 * Builds an [IrCall] expression for invoking a method on a target receiver.
 *
 * @param target The target expression to be used as the dispatch receiver, or `null` if the call has no receiver
 * @param methodSymbol The symbol of the function to be called
 * @param block Optional lambda for additional configuration of the [IrCall]
 * @return A configured [IrCall] instance with the dispatch receiver set to the target expression
 * @throws IllegalArgumentException if called outside a valid transformation scope
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(transformer: IrScopedTransformer)
internal fun buildIrCall(
    target: IrExpression?,
    methodSymbol: IrSimpleFunctionSymbol,
    block: IrCall.() -> Unit = {},
): IrCall {
    val currentScope = requireNotNull(transformer.scope) {
        "Not in a context when trying to build the a IrCall."
    }
    val builder = DeclarationIrBuilder(
        generatorContext = transformer.pluginContext,
        symbol = currentScope.scopeOwnerSymbol,
        startOffset = target?.startOffset ?: UNDEFINED_OFFSET,
        endOffset = target?.endOffset ?: UNDEFINED_OFFSET,
    )
    return builder.irCall(callee = methodSymbol).apply {
        dispatchReceiver = target
        block()
    }
}
