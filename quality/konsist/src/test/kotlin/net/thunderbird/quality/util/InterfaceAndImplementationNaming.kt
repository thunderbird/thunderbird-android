package net.thunderbird.quality.util

import com.lemonappdev.konsist.api.declaration.KoClassDeclaration
import com.lemonappdev.konsist.api.declaration.KoInterfaceDeclaration
import com.lemonappdev.konsist.api.declaration.combined.KoClassAndInterfaceAndObjectDeclaration
import com.lemonappdev.konsist.api.declaration.combined.KoClassAndObjectDeclaration
import com.lemonappdev.konsist.api.ext.list.sourceDeclarations

private val testSourceSet = Regex("""/src/([a-zA-Z]*[Tt]est[a-zA-Z]*)/""")

private val ignoredInterfaces = setOf(
    "net.thunderbird.components.ui.bolt.molecule.LoadingState",
    "net.thunderbird.components.ui.bolt.molecule.LoadingErrorState",
    "net.thunderbird.feature.account.Account",
)

private val variantSuffixRegex = Regex("""(Api|To)\d+$""")

fun KoClassAndObjectDeclaration.isInTestCode(): Boolean {
    val normalizedPath = path.replace('\\', '/')
    return testSourceSet.containsMatchIn(normalizedPath)
}

/**
 * Returns the direct project declarations referenced by this declaration.
 */
private fun KoClassAndInterfaceAndObjectDeclaration.sourceParentDeclarations():
    List<KoClassAndInterfaceAndObjectDeclaration> =
    parents(indirectParents = false)
        .sourceDeclarations()
        .filterIsInstance<KoClassAndInterfaceAndObjectDeclaration>()

/**
 * Determines whether this declaration is sealed.
 */
private fun KoClassAndInterfaceAndObjectDeclaration.isSealedDeclaration(): Boolean =
    when (this) {
        is KoInterfaceDeclaration -> hasSealedModifier
        is KoClassDeclaration -> hasSealedModifier
        else -> false
    }

/**
 * Returns all implemented project interfaces, including inherited interfaces.
 *
 * Sealed declarations are excluded because their subclasses represent cases
 * of a sealed hierarchy rather than conventional interface implementations.
 *
 * Capability interfaces are excluded without traversing their parent interfaces.
 */
private fun KoClassAndObjectDeclaration.implementedInterfaces():
    Set<KoInterfaceDeclaration> {

    val visited = mutableSetOf<KoClassAndInterfaceAndObjectDeclaration>()

    fun collectInterfaces(
        declaration: KoClassAndInterfaceAndObjectDeclaration,
    ): Set<KoInterfaceDeclaration> {
        if (!visited.add(declaration)) {
            return emptySet()
        }

        return declaration.sourceParentDeclarations()
            .flatMap { parent ->
                when {
                    parent.isSealedDeclaration() -> emptySet()

                    parent is KoInterfaceDeclaration -> {
                        if (parent.fullyQualifiedName?.let(ignoredInterfaces::contains) == true) {
                            emptySet()
                        } else {
                            setOf(parent) + collectInterfaces(parent)
                        }
                    }

                    else -> collectInterfaces(parent)
                }
            }
            .toSet()
    }

    // A direct sealed parent makes this declaration a sealed-hierarchy case.
    if (sourceParentDeclarations().any { it.isSealedDeclaration() }) {
        return emptySet()
    }

    return collectInterfaces(this)
}

/**
 * Returns true when the declaration has exactly one effective direct
 * project interface.
 *
 * Interfaces inherited through another interface do not count as additional
 * direct interfaces.
 */
fun KoClassAndObjectDeclaration.implementsSingleInterface(): Boolean {
    val interfaces = implementedInterfaces()

    val extendedInterfaces = interfaces
        .flatMap { iface ->
            iface.sourceParentDeclarations()
                .filterIsInstance<KoInterfaceDeclaration>()
        }
        .toSet()

    return (interfaces - extendedInterfaces).size == 1
}

/**
 * Returns interface names used for reporting and validation.
 */
private fun KoClassAndObjectDeclaration.implementedInterfaceNames(): List<String> =
    implementedInterfaces()
        .map { it.name }
        .sorted()

/**
 * Removes the package portion from a fully qualified declaration name.
 *
 * Nested declaration names remain intact.
 */
private fun KoClassAndInterfaceAndObjectDeclaration.relativeQualifiedName(): String {
    val qualifiedName = fullyQualifiedName ?: return name
    val packageName = packagee?.name.orEmpty()

    return if (packageName.isNotEmpty()) {
        qualifiedName.removePrefix("$packageName.")
    } else {
        qualifiedName
    }
}

/**
 * Flattens nested declaration names.
 *
 * Example:
 * GlobalCssStyleProvider.Factory -> GlobalCssStyleProviderFactory
 */
private fun String.flattenQualifiedName(): String =
    replace(".", "")

/**
 * Returns the enclosing declaration names of an interface.
 */
private fun KoInterfaceDeclaration.enclosingDeclarationNames(): List<String> {
    val relativeName = relativeQualifiedName()

    return relativeName
        .split('.')
        .dropLast(1)
}

/**
 * Checks whether an interface is nested inside a Contract declaration.
 */
private fun KoInterfaceDeclaration.isInsideContract(): Boolean =
    enclosingDeclarationNames().any { it.endsWith("Contract") }

/**
 * Returns the group name for an interface nested inside a contract group.
 *
 * Example:
 * FundingDataContract.Mapper.ContributionPurchase
 *
 * Group name: Mapper
 */
private fun KoInterfaceDeclaration.contractGroupName(): String? {
    val enclosingNames = enclosingDeclarationNames()

    if (enclosingNames.size < 2) {
        return null
    }

    val groupName = enclosingNames.last()
    val contractName = enclosingNames[enclosingNames.lastIndex - 1]

    return groupName.takeIf {
        contractName.endsWith("Contract") &&
            !groupName.endsWith("Contract")
    }
}

/**
 * Determines whether the implementation name follows the naming convention
 * for any of its implemented interfaces.
 *
 * Supported naming patterns include:
 *
 * - DefaultMessageStore
 * - FakeMessageStore
 * - BaseMessageStore
 * - AlarmPermissionManagerApi31
 * - CombinedSettingsUpgraderTo100
 * - ContributionPurchaseMapper
 * - DefaultGlobalCssStyleProviderFactory
 */
fun KoClassAndObjectDeclaration.matchesImplementedInterface(): Boolean {
    val interfaces = implementedInterfaces()

    if (interfaces.isEmpty()) {
        return false
    }

    val name = name
    val variantBaseName = name.replace(variantSuffixRegex, "")
    val hasVariantSuffix = variantBaseName != name

    val qualifiedName = relativeQualifiedName()
        .flattenQualifiedName()

    val qualifiedVariantBaseName = qualifiedName
        .replace(variantSuffixRegex, "")

    return interfaces.any { iface ->
        val interfaceName = iface.name
        val qualifiedInterfaceName = iface.relativeQualifiedName()
            .flattenQualifiedName()

        val matchesSimpleName =
            variantBaseName.endsWith(interfaceName) &&
                (variantBaseName != interfaceName || hasVariantSuffix)

        val matchesQualifiedName =
            qualifiedVariantBaseName.endsWith(qualifiedInterfaceName) &&
                (
                    qualifiedVariantBaseName != qualifiedInterfaceName ||
                        hasVariantSuffix
                    )

        // A class can share the interface name when the interface is
        // enclosed by a Contract declaration.
        val matchesContractName =
            iface.isInsideContract() &&
                (
                    name == interfaceName ||
                        qualifiedName == qualifiedInterfaceName
                    )

        // An interface nested in a contract group can use:
        // <InterfaceName><GroupName>
        val groupName = iface.contractGroupName()

        val matchesContractGroup =
            groupName != null &&
                (
                    variantBaseName.endsWith(interfaceName + groupName) ||
                        qualifiedVariantBaseName.endsWith(
                            qualifiedInterfaceName + groupName,
                        )
                    )

        matchesSimpleName ||
            matchesQualifiedName ||
            matchesContractName ||
            matchesContractGroup
    }
}

/**
 * Asserts that no invalid implementation names exist.
 */
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

            (
                listOf("$className -> ${suggestionFor(className, interfaceNames)}") +
                    locations
                ).joinToString(separator = "\n")
        }

    throw AssertionError(
        "${violations.size} invalid implementation names. $message\n\n$report",
    )
}

/**
 * Asserts that no invalid interface names exist.
 */
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

            (
                listOf("$interfaceName -> ${suggestionFor(interfaceName)}") +
                    locations
                ).joinToString(separator = "\n")
        }

    throw AssertionError(
        "${violations.size} invalid interface names. $message\n\n$report",
    )
}
