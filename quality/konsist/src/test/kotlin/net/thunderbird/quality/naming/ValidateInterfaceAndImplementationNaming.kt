package net.thunderbird.quality.naming

import com.lemonappdev.konsist.api.declaration.KoClassDeclaration
import kotlin.test.Test
import net.thunderbird.quality.util.assertNoInvalidImplementationNames
import net.thunderbird.quality.util.assertNoInvalidInterfaceNames
import net.thunderbird.quality.util.isInTestCode

class ValidateInterfaceAndImplementationNaming {

    @Test
    fun `interfaces should not use the I prefix`() {
        assertNoInvalidInterfaceNames(
            message = "Avoid the IInterface pattern (ADR-0004).",
            suggestionFor = { name ->
                "Name the interface as if it were a class: ${name.removePrefix("I")}"
            },
            isInvalid = { name ->
                // Keep acronym-based names such as IMAPStore and IURLProvider valid.
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
                when {
                    interfaces.size == 1 ->
                        "Use <Prefix>${interfaces.first()} instead of $name"

                    interfaces.isNotEmpty() ->
                        "Use a descriptive prefix with one of these interfaces: ${interfaces.joinToString()}"

                    else ->
                        "Remove the Impl suffix from $name"
                }
            },
            isInvalid = { clazz, _ ->
                clazz.name.endsWith("Impl")
            },
        )
    }

    @Test
    fun `implementations should be a prefix plus the interface name`() {
        assertNoInvalidImplementationNames(
            message = """
                Name implementations <Prefix><InterfaceName> (ADR-0004), e.g.:
                - A descriptive prefix: DatabaseMessageStore, InMemoryMessageStore
                - Android for platform-specific implementations
                - K9 / Tb for app-specific implementations
                - Default when no brief descriptive name fits
                - Fake for test doubles (test code only): FakeMessageStore
                - Base for abstract or open base classes: BaseMessageStore
            """.trimIndent(),
            suggestionFor = { name, interfaces ->
                when {
                    interfaces.size == 1 ->
                        "Rename $name to <Prefix>${interfaces.first()}"

                    interfaces.isNotEmpty() ->
                        "Rename $name using a descriptive prefix. Implemented interfaces: ${interfaces.joinToString()}"

                    else ->
                        "Rename $name using a meaningful descriptive name"
                }
            },
            isInvalid = { clazz, interfaces ->
                val name = clazz.name
                if (name.endsWith("Test")) {
                    false
                } else {
                    val matchesInterface = interfaces.any { iface ->
                        name.endsWith(iface) && name != iface
                    }

                    val hasFakePrefix = name.hasPrefixWord("Fake")
                    val hasBasePrefix = name.hasPrefixWord("Base")

                    val isValidFake = hasFakePrefix &&
                        clazz.isInTestCode() &&
                        matchesInterface

                    val isValidBase = hasBasePrefix &&
                        clazz is KoClassDeclaration &&
                        (clazz.hasAbstractModifier || clazz.hasOpenModifier) &&
                        matchesInterface

                    val isValidDescriptiveName = !hasFakePrefix &&
                        !hasBasePrefix &&
                        matchesInterface

                    !isValidFake && !isValidBase && !isValidDescriptiveName
                }
            },
        )
    }
}

private fun String.hasPrefixWord(prefix: String): Boolean =
    startsWith(prefix) && getOrNull(prefix.length)?.isUpperCase() == true
