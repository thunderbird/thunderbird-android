package net.thunderbird.core.common.provider

/**
 * Represents a provider of context.
 *
 * @param TContext The type of context provided.
 */
interface ContextProvider<TContext> {
    val context: TContext
}
