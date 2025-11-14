package net.thunderbird.core.common.inject

import org.koin.core.definition.Definition
import org.koin.core.module.KoinDslMarker
import org.koin.core.module.Module
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.Qualifier
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope

// This file must be deleted once https://github.com/InsertKoinIO/koin/pull/1951 is merged to
// Koin and released on 4.2.0

/**
 * Defines a singleton list of elements of type [T].
 *
 * This function creates a singleton definition for a mutable list of elements.
 * Each element in the list is resolved from the provided [items] definitions.
 *
 * @param T The type of elements in the list.
 * @param items Vararg of [Definition]s that will be resolved and added to the list.
 * @param qualifier Optional [Qualifier] to distinguish this list from others of the same type.
 *                  If null, a default qualifier based on the type [T] will be used.
 */
@KoinDslMarker
inline fun <reified T> Module.singleListOf(vararg items: Definition<T>, qualifier: Qualifier? = null) {
    single(qualifier ?: defaultListQualifier<T>(), createdAtStart = true) {
        items.map { definition -> definition(this, parametersOf()) }
    }
}

/**
 * Defines a factory for a list of elements of type [T].
 *
 * This function creates a factory definition for a list of elements. Each time this list is
 * requested, a new list is created, and each element in the list is resolved anew from the
 * provided [items] definitions.
 *
 * @param T The type of elements in the list.
 * @param items Vararg of [Definition]s that will be resolved and added to the list.
 * @param qualifier Optional [Qualifier] to distinguish this list from others of the same type.
 *                  If null, a default qualifier based on the type [T] will be used.
 */
@KoinDslMarker
inline fun <reified T> Module.factoryListOf(vararg items: Definition<T>, qualifier: Qualifier? = null) {
    factory(qualifier ?: defaultListQualifier<T>()) {
        items.map { definition -> definition(this, parametersOf()) }
    }
}

/**
 * Resolves a [List] of instances of type [T].
 * This is a helper function for Koin's multibinding feature.
 *
 * It uses the [defaultListQualifier] if no [qualifier] is provided.
 *
 * @param T The type of instances in the list.
 * @param qualifier An optional [Qualifier] to distinguish between different lists of the same type.
 * @return The resolved [MutableList] of instances of type [T].
 */
inline fun <reified T> Scope.getList(qualifier: Qualifier? = null) =
    get<List<T>>(qualifier ?: defaultListQualifier<T>())

/**
 * Creates a qualifier for a set of a specific type.
 *
 * This is used to differentiate between different sets of the same type when injecting dependencies.
 *
 * @param T The type of the elements in the set.
 * @return A qualifier for the set.
 */
inline fun <reified T> defaultListQualifier() =
    defaultCollectionQualifier<List<T>, T>()

/**
 * Creates a default [Qualifier] for a collection binding.
 *
 * @param TCollection The type of the collection (e.g., `List`, `List`).
 * @param T The type of the elements in the collection.
 * @return A [Qualifier] that can be used to identify the specific collection binding.
 */
inline fun <reified TCollection : Collection<T>, reified T> defaultCollectionQualifier() =
    named("${TCollection::class.qualifiedName}<${T::class.qualifiedName}>")
