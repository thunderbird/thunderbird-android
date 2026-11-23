package net.thunderbird.quality.ui

import com.lemonappdev.konsist.api.declaration.KoClassDeclaration
import com.lemonappdev.konsist.api.declaration.KoInterfaceDeclaration
import com.lemonappdev.konsist.api.declaration.KoParentDeclaration
import com.lemonappdev.konsist.api.ext.list.withName
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import kotlin.test.Test
import net.thunderbird.quality.util.assertNoViolations
import net.thunderbird.quality.util.productionScope

/**
 * Enforces the MVI conventions of `docs/architecture/ui-architecture.md` and the `ui` package layout of
 * `docs/architecture/module-structure.md`.
 *
 * The last rule adds to the detekt Compose rule `ViewModelForwarding`, which only reports a composable that passes its
 * ViewModel on to another composable. This rule also reports a composable that takes a ViewModel and uses it itself.
 */
class ValidateMviConventions {

    private val viewModels = productionScope.classes()
        .withNameEndingWith(VIEW_MODEL)
        .filterNot { it.hasAbstractModifier }

    @Test
    fun `view models should extend an MVI base view model`() {
        assertNoViolations(
            message = "Extend $BASE_VIEW_MODEL or $BASE_STATE_MACHINE_VIEW_MODEL (see the ViewModel section of $DOC).",
            declarations = viewModels.filterNot { it.hasParentIn(BASE_VIEW_MODELS) },
            baseline = MviBaseline.viewModelsWithoutBase,
        )
    }

    @Test
    fun `ui contracts should declare State, Event and Effect`() {
        val uiContracts = productionScope.interfaces(includeNested = false)
            .withNameEndingWith("Contract")
            .filter { contract -> contract.nestedNames().contains(VIEW_MODEL) }

        assertNoViolations(
            message = "A contract with a nested ViewModel defines the screen's State, Event and Effect, either nested " +
                "or as the type arguments of an MVI base type (see the MVI section of $DOC).",
            declarations = uiContracts.filterNot { contract ->
                contract.nestedNames().containsAll(CONTRACT_TYPES) || contract.hasTypedViewModel()
            },
            baseline = MviBaseline.incompleteUiContracts,
        )
    }

    @Test
    fun `view models should reside in a ui package`() {
        assertNoViolations(
            message = "Move the ViewModel into a `ui` package (see the Clean Architecture in Internal Modules section of " +
                "$MODULE_DOC).",
            declarations = viewModels.filterNot { it.resideInPackage(UI_PACKAGE) },
            baseline = MviBaseline.viewModelsOutsideUiPackage,
        )
    }

    @Test
    fun `screens should reside in a ui package`() {
        assertNoViolations(
            message = "Move the screen composable into a `ui` package (see the Clean Architecture in Internal Modules " +
                "section of $MODULE_DOC).",
            declarations = composables().filter { it.name.endsWith("Screen") && !it.resideInPackage(UI_PACKAGE) },
            baseline = MviBaseline.screensOutsideUiPackage,
        )
    }

    @Test
    fun `only screens should take a view model`() {
        assertNoViolations(
            message = "Pass `state` and `onEvent` instead of the ViewModel; only the top-level *Screen " +
                "composable, or an entry point that injects its ViewModel with `koinViewModel()`, gets the " +
                "ViewModel (see the Screens section of $DOC).",
            declarations = composables().filter { function ->
                !function.name.endsWith("Screen") &&
                    function.parameters.any { parameter ->
                        parameter.type.name.removeSuffix("?").endsWith(VIEW_MODEL) &&
                            parameter.defaultValue?.startsWith(KOIN_VIEW_MODEL) != true
                    }
            },
            baseline = MviBaseline.composablesTakingViewModel,
        )
    }

    private fun composables() = productionScope.functions(includeNested = true, includeLocal = false)
        .filter { it.hasAnnotationWithName("Composable") }

    /** Whether this class extends, directly or not, one of the classes or interfaces named in [names]. */
    private fun KoClassDeclaration.hasParentIn(names: Set<String>) =
        parents(indirectParents = true).any { parent -> parent.fullyQualifiedName() in names }

    /**
     * Whether the contract's nested ViewModel extends a shared MVI type. Its type arguments then name the screen's
     * State, Event and Effect, even when they are declared outside the contract.
     */
    private fun KoInterfaceDeclaration.hasTypedViewModel(): Boolean {
        val viewModel = classes(includeNested = false).withName(VIEW_MODEL).firstOrNull()
            ?: interfaces(includeNested = false).withName(VIEW_MODEL).firstOrNull()
        val parents = when (viewModel) {
            is KoClassDeclaration -> viewModel.parents(indirectParents = true)
            is KoInterfaceDeclaration -> viewModel.parents(indirectParents = true)
            else -> emptyList()
        }
        return parents.any { it.fullyQualifiedName() in MVI_TYPES }
    }

    private fun KoParentDeclaration.fullyQualifiedName(): String? = when (val source = sourceDeclaration) {
        is KoClassDeclaration -> source.fullyQualifiedName
        is KoInterfaceDeclaration -> source.fullyQualifiedName
        else -> null
    }

    private fun KoInterfaceDeclaration.nestedNames(): Set<String> =
        classesAndObjects(includeNested = false).map { it.name }.toSet() +
            interfaces(includeNested = false).map { it.name }

    private companion object {
        const val VIEW_MODEL = "ViewModel"
        const val BASE_VIEW_MODEL = "net.thunderbird.core.ui.contract.mvi.BaseViewModel"
        const val BASE_STATE_MACHINE_VIEW_MODEL = "net.thunderbird.core.ui.compose.common.mvi.BaseStateMachineViewModel"
        val BASE_VIEW_MODELS = setOf(BASE_VIEW_MODEL, BASE_STATE_MACHINE_VIEW_MODEL)
        val MVI_TYPES = BASE_VIEW_MODELS + "net.thunderbird.core.ui.contract.mvi.UnidirectionalViewModel"
        const val KOIN_VIEW_MODEL = "koinViewModel"
        val CONTRACT_TYPES = setOf("State", "Event", "Effect")
        const val UI_PACKAGE = "..ui.."
        const val DOC = "docs/architecture/ui-architecture.md"
        const val MODULE_DOC = "docs/architecture/module-structure.md"
    }
}
