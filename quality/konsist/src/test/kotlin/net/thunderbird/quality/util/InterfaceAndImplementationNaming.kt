package net.thunderbird.quality.util

import com.lemonappdev.konsist.api.declaration.KoInterfaceDeclaration
import com.lemonappdev.konsist.api.declaration.combined.KoClassAndInterfaceAndObjectDeclaration
import com.lemonappdev.konsist.api.declaration.combined.KoClassAndObjectDeclaration
import com.lemonappdev.konsist.api.ext.list.sourceDeclarations

private val testPathMarkers = listOf(
    "/src/test/",
    "/src/androidTest/",
    "/src/testFixtures/",
    "/src/commonTest/",
    "/src/jvmTest/",
    "/src/androidUnitTest/",
    "/src/androidHostTest/",
)

fun KoClassAndObjectDeclaration.isInTestCode(): Boolean {
    val normalizedPath = path.replace('\\', '/')

    return testPathMarkers.any { normalizedPath.contains(it) }
}

private fun KoClassAndObjectDeclaration.implementedInterfaceNames(): List<String> {
    val visited = mutableSetOf<KoClassAndInterfaceAndObjectDeclaration>()

    fun collectInterfaces(
        declaration: KoClassAndInterfaceAndObjectDeclaration,
    ): Set<String> {
        if (!visited.add(declaration)) {
            return emptySet()
        }

        return declaration.parents(indirectParents = false)
            .sourceDeclarations()
            .filterIsInstance<KoClassAndInterfaceAndObjectDeclaration>()
            .flatMap { parent ->
                when (parent) {
                    is KoInterfaceDeclaration ->
                        setOf(parent.name) + collectInterfaces(parent)

                    else ->
                        collectInterfaces(parent)
                }
            }
            .toSet()
    }

    return collectInterfaces(this).sorted()
}

fun assertNoInvalidImplementationNames(
    message: String,
    suggestionFor: (className: String, interfaceNames: List<String>) -> String,
    isInvalid: (
        clazz: KoClassAndObjectDeclaration,
        interfaceNames: List<String>,
    ) -> Boolean,
) {
    val violations = (projectScope.classes() + projectScope.objects())
        .filter { clazz ->
            val interfaceNames = clazz.implementedInterfaceNames()
            isInvalid(clazz, interfaceNames)
        }

    if (violations.isEmpty()) return

    val report = violations
        .groupBy { it.name }
        .toSortedMap()
        .entries
        .joinToString(separator = "\n") { (className, classes) ->
            val interfaceNames = classes
                .flatMap { it.implementedInterfaceNames() }
                .distinct()
                .sorted()

            val locations = classes
                .map { "  file://${it.location}" }
                .sorted()

            (listOf("$className -> ${suggestionFor(className, interfaceNames)}") + locations)
                .joinToString(separator = "\n")
        }

    throw AssertionError(
        "${violations.size} invalid implementation names. $message\n\n$report",
    )
}

fun assertNoInvalidInterfaceNames(
    message: String,
    suggestionFor: (interfaceName: String) -> String,
    isInvalid: (interfaceName: String) -> Boolean,
) {
    val violations = projectScope.interfaces()
        .filter { isInvalid(it.name) }

    if (violations.isEmpty()) return

    val report = violations
        .groupBy { it.name }
        .toSortedMap()
        .entries
        .joinToString(separator = "\n") { (interfaceName, interfaces) ->
            val locations = interfaces
                .map { "  file://${it.location}" }
                .sorted()

            (listOf("$interfaceName -> ${suggestionFor(interfaceName)}") + locations)
                .joinToString(separator = "\n")
        }

    throw AssertionError(
        "${violations.size} invalid interface names. $message\n\n$report",
    )
}
