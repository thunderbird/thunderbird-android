package net.thunderbird.quality.util

import com.lemonappdev.konsist.api.declaration.KoClassDeclaration
import com.lemonappdev.konsist.api.ext.list.sourceDeclarations

private val testPathMarkers = listOf("/src/test/", "/src/androidTest/", "/src/testFixtures/")

fun KoClassDeclaration.isInTestCode(): Boolean {
    val normalizedPath = path.replace('\\', '/')
    return testPathMarkers.any { normalizedPath.contains(it) } ||
        resideInPackage("..testing..")
}

private fun KoClassDeclaration.implementedInterfaceNames(): List<String> =
    parents()
        .sourceDeclarations()
        .filter { it.isInterface }
        .map { it.name }

fun assertNoInvalidImplementationNames(
    message: String,
    suggestionFor: (className: String, interfaceNames: List<String>) -> String,
    isInvalid: (clazz: KoClassDeclaration, interfaceNames: List<String>) -> Boolean,
) {
    val violations = projectScope.classes()
        .filter { clazz ->
            val interfaceNames = clazz.implementedInterfaceNames()
            !clazz.hasDataModifier &&
                interfaceNames.isNotEmpty() &&
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

