package net.thunderbird.core.featureflag.inject.qualifier

/**
 * Qualifier for dependency injection to distinguish between different implementation types.
 *
 * Used to differentiate between in-memory, local, and remote implementations when
 * injecting dependencies through a dependency injection framework.
 */
enum class InjectQualifier { InMemory, Local, Remote }
