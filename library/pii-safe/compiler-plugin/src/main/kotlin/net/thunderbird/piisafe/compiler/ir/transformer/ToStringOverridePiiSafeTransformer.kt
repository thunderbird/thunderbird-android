package net.thunderbird.piisafe.compiler.ir.transformer

import net.thunderbird.piisafe.compiler.ir.ToStringOverridePiiSafeBodyGenerator
import net.thunderbird.piisafe.compiler.ir.builder.buildIrCall
import net.thunderbird.piisafe.compiler.symbols.ProjectClassIds
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrStringConcatenationImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.hasAnnotation

internal class ToStringOverridePiiSafeTransformer(override val pluginContext: IrPluginContext) : IrScopedTransformer() {

    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
        val owner = declaration.parent
        if (declaration.origin == ToStringOverridePiiSafeBodyGenerator.Origin && owner is IrClass && owner.isData) {
            val returnType = declaration.returnType
            declaration.body =
                pluginContext.irFactory.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET).apply {
                    statements += IrReturnImpl(
                        startOffset = UNDEFINED_OFFSET,
                        endOffset = UNDEFINED_OFFSET,
                        type = returnType,
                        returnTargetSymbol = declaration.symbol,
                        value = createToStringPiiSafeIrExpression(
                            pluginContext = pluginContext,
                            simpleFunction = declaration,
                            owner = owner,
                            returnType = returnType,
                        ),
                    )
                }
        }

        return super.visitSimpleFunction(declaration)
    }

    private fun createToStringPiiSafeIrExpression(
        pluginContext: IrPluginContext,
        simpleFunction: IrSimpleFunction,
        owner: IrClass,
        returnType: IrType,
    ): IrExpression = IrStringConcatenationImpl(
        startOffset = UNDEFINED_OFFSET,
        endOffset = UNDEFINED_OFFSET,
        type = returnType,
        arguments = owner.generateToStringPiiSafeValue(pluginContext, simpleFunction),
    )

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrClass.generateToStringPiiSafeValue(
        pluginContext: IrPluginContext,
        simpleFunction: IrSimpleFunction,
    ): List<IrExpression> = buildList {
        val className = name.asString()
        val expressions = declarations
            .asSequence()
            .filterIsInstance<IrProperty>()
            .mapNotNull { property ->
                property.getter?.let { getter ->
                    property to when {
                        getter.hasAnnotation(ProjectClassIds.PiiSafeMaskClassId) ->
                            pluginContext.createStringIrConst("<sensitive>")

                        getter.hasAnnotation(ProjectClassIds.PiiSafeHideClassId) -> null

                        else -> property.createPlainIrCall(simpleFunction)
                    }
                }
            }
            .toList()

        add(pluginContext.createStringIrConst("$className("))
        var i = 0
        for ((property, expression) in expressions) {
            expression ?: continue
            if (i++ > 0) {
                add(pluginContext.createStringIrConst(", "))
            }
            add(pluginContext.createStringIrConst("${property.name} = "))
            add(expression)
        }
        val hiddenProps = expressions.count { (_, expression) -> expression == null }
        if (hiddenProps > 0) {
            add(
                pluginContext.createStringIrConst(
                    value = "${if (i > 0) ", " else ""}+$hiddenProps hidden properties",
                ),
            )
        }
        add(pluginContext.createStringIrConst(")"))
    }

    private fun IrPluginContext.createStringIrConst(value: String): IrConstImpl = IrConstImpl(
        startOffset = UNDEFINED_OFFSET,
        endOffset = UNDEFINED_OFFSET,
        type = irBuiltIns.stringType,
        kind = IrConstKind.String,
        value = value,
    )

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrProperty.createPlainIrCall(simpleFunction: IrSimpleFunction): IrCall = requireNotNull(
        getter?.let { getter ->
            val target = if (getter.parameters.any { it.kind == IrParameterKind.DispatchReceiver }) {
                val thisArg = requireNotNull(simpleFunction.dispatchReceiverParameter)
                IrGetValueImpl(
                    startOffset = UNDEFINED_OFFSET,
                    endOffset = UNDEFINED_OFFSET,
                    type = thisArg.type,
                    symbol = thisArg.symbol,
                )
            } else {
                null
            }
            buildIrCall(
                target = target,
                methodSymbol = getter.symbol,
            )
        },
    ) {
        "Property '$name' does not contain getter."
    }
}
