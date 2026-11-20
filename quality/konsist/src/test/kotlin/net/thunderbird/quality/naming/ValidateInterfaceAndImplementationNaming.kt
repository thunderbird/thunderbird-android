package net.thunderbird.quality.naming

import kotlin.test.Ignore
import kotlin.test.Test
import net.thunderbird.quality.util.assertNoInvalidImplementationNames
import net.thunderbird.quality.util.assertNoInvalidInterfaceNames
import net.thunderbird.quality.util.isInTestCode

// TODO "Remove the @Ignore after applying these rule across project"
@Ignore("This rules are yet to apply across project")
class ValidateInterfaceAndImplementationNaming {

    @Test
    fun `interfaces should not use the I prefix`() {
        assertNoInvalidInterfaceNames(
            message = "Avoid the IInterface pattern (ADR-0004).",
            suggestionFor = { name ->
                "Name the interface as if it were a class: ${name.removePrefix("I")}"
            },
            isInvalid = { name ->
                name.length > 2 &&
                    name[0] == 'I' &&
                    name[1].isUpperCase() &&
                    name[2].isLowerCase()
            },
        )
    }

    @Test
    fun `implementations should not use the Impl suffix`() {
        assertNoInvalidImplementationNames(
            message = "Avoid the Impl suffix (ADR-0004).",
            suggestionFor = { name, interfaces ->
                "Use <Prefix>${interfaces.first()} instead of $name"
            },
            isInvalid = { clazz, _ -> clazz.name.endsWith("Impl") },
        )
    }

    @Test
    fun `implementations should be a prefix plus the interface name`() {
        assertNoInvalidImplementationNames(
            message = """
                Name implementations <Prefix><InterfaceName> (ADR-0004), e.g.:
                - a descriptive prefix: DatabaseMessageStore, InMemoryMessageStore
                - Android for platform-specific implementations
                - K9 / Tb for app-specific implementations
                - Default when no brief descriptive name fits
                - Fake for test doubles (test code only): FakeMessageStore
                - Base for abstract or open base classes: BaseMessageStore
            """.trimIndent(),
            suggestionFor = { name, interfaces ->
                val interfaceDescription = interfaces.joinToString()

                if (interfaces.size == 1) {
                    "Rename $name to <Prefix>${interfaces.first()}"
                } else {
                    "Rename $name using a descriptive prefix. Implemented interfaces: $interfaceDescription"
                }
            },
            isInvalid = { clazz, interfaces ->
                val name = clazz.name
                val isFakeInTests = clazz.isInTestCode() &&
                    name.hasPrefixWord("Fake") &&
                    interfaces.any { name.endsWith(it) }

                val isExtensibleBase  = (clazz.hasAbstractModifier || clazz.hasOpenModifier) &&
                    name.hasPrefixWord("Base") &&
                    interfaces.any { iface -> name.endsWith(iface) }

                val matchesInterface = interfaces.any { iface ->
                    name.endsWith(iface) && name != iface
                }

                !isFakeInTests && !isExtensibleBase  && !matchesInterface
            },
        )
    }
}

private fun String.hasPrefixWord(prefix: String): Boolean =
    startsWith(prefix) && getOrNull(prefix.length)?.isUpperCase() == true
