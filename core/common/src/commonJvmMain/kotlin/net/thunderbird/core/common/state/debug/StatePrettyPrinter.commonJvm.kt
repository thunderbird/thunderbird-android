package net.thunderbird.core.common.state.debug

import java.lang.reflect.Modifier

internal actual fun <T : Any> T.toPropertyMap(): Map<String, Any?> {
    if (this is Collection<*> || this is Map<*, *>) return emptyMap()

    val fields = this::class.java
        .declaredFields
        .filter { !Modifier.isStatic(it.modifiers) && !it.isSynthetic }
        .sortedBy { it.name }

    return buildMap {
        for (field in fields) {
            try {
                field.isAccessible = true
                put(key = field.name, value = field[this@toPropertyMap])
            } catch (_: RuntimeException) {
                // Skip fields that cannot be accessed (e.g., platform types in named modules)
            }
        }
    }
}
