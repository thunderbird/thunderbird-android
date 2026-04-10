package net.thunderbird.core.common.state.debug

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
            } catch (_: RuntimeException) {
                // Skip fields that cannot be accessed (e.g., platform types in named modules)
            }
        }
    }
}
