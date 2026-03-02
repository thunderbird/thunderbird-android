package net.thunderbird.core.common.state.debug

import java.lang.reflect.InaccessibleObjectException

// TODO: We should review this after the AGP 9 update happens.
// Read the KDoc at the expect declaration.
internal actual fun <T : Any> T.toPropertyMap(): Map<String, Any?> {
    if (this is Collection<*> || this is Map<*, *>) return emptyMap()

    val fields = this::class.java
        .declaredFields
        .filter { !java.lang.reflect.Modifier.isStatic(it.modifiers) && !it.isSynthetic }
        .sortedBy { it.name }

    return buildMap {
        for (field in fields) {
            try {
                field.isAccessible = true
                put(field.name, field[this@toPropertyMap])
            } catch (_: InaccessibleObjectException) {
                // Skip fields that cannot be accessed (e.g., platform types in named modules)
            }
        }
    }
}
