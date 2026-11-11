package net.thunderbird.quality.di

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import com.lemonappdev.konsist.api.declaration.KoPropertyDeclaration
import kotlin.test.Ignore
import kotlin.test.Test
import net.thunderbird.quality.util.QualityViolation
import net.thunderbird.quality.util.assertNoQualityViolations
import net.thunderbird.quality.util.testScope
import org.junit.AfterClass
import org.junit.BeforeClass

class ValidateKoinModules {

    @Test
    fun `koin module files should be named after their module`() {
        val violations = moduleFiles
            .flatMap { (file, modules) ->
                modules.mapNotNull { module ->
                    koinModuleNamingViolation(
                        moduleName = module.name,
                        fileName = file.path.substringAfterLast('/'),
                        location = module.location,
                    )
                }
            }
            .toSet()

        assertNoViolations(
            name = "Naming",
            message = "Name Koin modules <feature>DiModule and their files <Feature>DiModule.kt: " +
                "`val fooDiModule = module { }` goes in FooDiModule.kt.",
            violations = violations,
        )
    }

    @Test
    fun `gradle modules should expose one public di entry point`() {
        val declarations = moduleFiles.flatMap { (_, modules) -> modules }.filter { it.isTopLevel }
        val violations = koinModuleVisibilityViolations(
            declarations.map { it.toKoinModuleVisibility() },
        )

        assertNoViolations(
            name = "Visibility",
            message = "Expose exactly one public DI entry point per Gradle module; mark supporting DI modules internal.",
            violations = violations.toSet(),
        )
    }

    @Test
    fun `koin module files should declare one module`() {
        val violations = moduleFiles
            .filter { (_, modules) -> modules.size > 1 }
            .flatMap { (_, modules) ->
                modules.map { module ->
                    QualityViolation(
                        location = module.location,
                        reason = "Multiple modules in one file: ${modules.joinToString { it.name }}.",
                        suggestion = "Move each module to its own file.",
                    )
                }
            }
            .toSet()

        assertNoViolations(
            name = "One per file",
            message = "Declare one Koin module per file. Move further modules to files named after them.",
            violations = violations,
        )
    }

    @Ignore
    @Test
    fun `koin module definitions should use the compiler plugin dsl`() {
        val violations = moduleFiles
            .flatMap { (file, modules) ->
                val imports = file.imports.filter { it.name in CLASSIC_DSL_IMPORTS }.map {
                    QualityViolation(
                        location = it.location,
                        reason = "Classic DSL import: ${it.name}.",
                        suggestion = "Use the compiler plugin DSL from org.koin.plugin.module.dsl.",
                    )
                }
                val resolutions = modules.flatMap { module ->
                    DSL_RESOLUTION_CALL.findAll(module.value.orEmpty()).map { match ->
                        QualityViolation(
                            location = module.location,
                            reason = "Module ${module.name} resolves dependencies with `${match.value}`.",
                            suggestion = "Use compiler plugin injection instead of resolution in definition lambdas.",
                        )
                    }.toList()
                }
                imports + resolutions
            }
            .toSet()

        assertNoViolations(
            name = "Compiler DSL",
            message = "Declare definitions with the Koin Compiler Plugin DSL from org.koin.plugin.module.dsl: " +
                "single<T>(), factory<T>(), viewModel<T>(), and single { create(::buildT) } for builders. " +
                "Don't resolve dependencies with get() or androidContext() in definition lambdas.",
            violations = violations,
        )
    }

    @Ignore
    @Test
    fun `koin graph tests should be named after the module they verify`() {
        val violations = testScope.classes()
            .filter {
                it.name.endsWith(GRAPH_TEST_SUFFIX) || it.name.endsWith(MODULE_KT_TEST_SUFFIX) ||
                    it.name.endsWith(MODULE_TEST_SUFFIX)
            }
            .filterNot { testClass ->
                val moduleName = testClass.name.removeSuffix("Test").replaceFirstChar(Char::lowercase)
                testClass.name.endsWith(DI_MODULE_TEST_SUFFIX) && MODULE_NAME.matches(moduleName) &&
                    Regex("""\b${Regex.escape(moduleName)}\b""").containsMatchIn(testClass.containingFile.text)
            }
            .map { testClass ->
                val moduleName = testClass.name.removeSuffix(GRAPH_TEST_SUFFIX)
                    .removeSuffix("KtTest").removeSuffix("Test")
                    .replaceFirstChar(Char::lowercase).toDiModuleName()
                QualityViolation(
                    location = testClass.location,
                    reason = "Test '${testClass.name}' does not match the module naming rule.",
                    suggestion = "Use '${moduleName.replaceFirstChar(Char::uppercase)}Test' and start $moduleName. " +
                        "Choose the feature name if it cannot be inferred.",
                )
            }
            .toSet()

        assertNoViolations(
            name = "Graph-test names",
            message = "Name Koin graph tests <Feature>DiModuleTest after the module they start, e.g. " +
                "ChangelogUiDiModuleTest for changelogUiDiModule.",
            violations = violations,
        )
    }

    private fun assertNoViolations(name: String, message: String, violations: Set<QualityViolation>) {
        ruleCounts[name] = violations.size
        assertNoQualityViolations(rule = message, violations = violations)
    }

    private companion object {
        val ruleCounts = mutableMapOf<String, Int>()

        val ruleMethods = linkedMapOf(
            "Naming" to "koin module files should be named after their module",
            "Visibility" to "gradle modules should expose one public di entry point",
            "One per file" to "koin module files should declare one module",
            "Compiler DSL" to "koin module definitions should use the compiler plugin dsl",
            "Graph-test names" to "koin graph tests should be named after the module they verify",
        )

        @BeforeClass
        @JvmStatic
        fun resetQualityCounts() {
            ruleCounts.clear()
        }

        @AfterClass
        @JvmStatic
        fun printQualitySummary() {
            val rules = ruleMethods.map { (name, method) ->
                KoinRuleCount(
                    name = name,
                    count = ruleCounts[name],
                    skipped = ValidateKoinModules::class.java.getDeclaredMethod(method)
                        .isAnnotationPresent(Ignore::class.java),
                )
            }
            println(renderKoinQualitySummary(rules))
        }

        const val GRAPH_TEST_SUFFIX = "GraphTest"
        const val MODULE_KT_TEST_SUFFIX = "ModuleKtTest"
        const val MODULE_TEST_SUFFIX = "ModuleTest"
        const val DI_MODULE_TEST_SUFFIX = "DiModuleTest"

        val DSL_RESOLUTION_CALL = Regex("""\b(get|getOrNull|getAll|androidContext|androidApplication)\s*(<.*?>)?\s*\(""")

        // Classic and Constructor DSL definitions; the Compiler Plugin DSL lives in org.koin.plugin.module.dsl
        val CLASSIC_DSL_IMPORTS = setOf(
            "org.koin.core.module.dsl.viewModel",
            "org.koin.core.module.dsl.viewModelOf",
            "org.koin.core.module.dsl.singleOf",
            "org.koin.core.module.dsl.factoryOf",
            "org.koin.core.module.dsl.scopedOf",
            "org.koin.androidx.viewmodel.dsl.viewModel",
            "org.koin.androidx.viewmodel.dsl.viewModelOf",
        )

        val MODULE_DECLARATION = Regex("""^module\s*[({]""")

        /** Production files with their top-level Koin module declarations (`val fooDiModule = module { }`). */
        val moduleFiles: List<Pair<KoFileDeclaration, List<KoPropertyDeclaration>>> =
            Konsist.scopeFromProduction().files
                .map { file -> file to file.properties().filter { MODULE_DECLARATION.containsMatchIn(it.value.orEmpty()) } }
                .filter { (_, modules) -> modules.isNotEmpty() }

    }
}

private val MODULE_NAME = Regex("""[a-z][A-Za-z0-9]*DiModule""")

private fun String.toDiModuleName(): String = when {
    MODULE_NAME.matches(this) -> this
    this == "module" || this == "koinModule" -> "<feature>DiModule"
    else -> removeSuffix("Module") + "DiModule"
}

internal fun koinModuleNamingViolation(moduleName: String, fileName: String, location: String): QualityViolation? {
    val expectedName = moduleName.toDiModuleName()
    val expectedFile = expectedName.replaceFirstChar(Char::uppercase) + "." + fileName.substringAfter('.')
    val propertyWrong = moduleName != expectedName
    val fileWrong = fileName != expectedFile
    if (!propertyWrong && !fileWrong) return null

    val corrections = buildList {
        if (propertyWrong) add("Rename property '$moduleName' to '$expectedName'.")
        if (fileWrong) add("Rename file '$fileName' to '$expectedFile'.")
    }
    return QualityViolation(
        location = location,
        reason = when {
            propertyWrong && fileWrong -> "Module property and filename do not follow the naming rule."
            propertyWrong -> "Module property does not follow the naming rule."
            else -> "Filename does not match the DI module name."
        },
        suggestion = corrections.joinToString(separator = " "),
    )
}

internal data class KoinRuleCount(val name: String, val count: Int? = null, val skipped: Boolean = false)

internal fun renderKoinQualitySummary(rules: List<KoinRuleCount>): String {
    val width = rules.maxOfOrNull { it.name.length } ?: 0
    return buildString {
        append("Koin quality checks")
        rules.forEach { rule ->
            val result = when {
                rule.count != null -> "${rule.count} ${if (rule.count == 1) "violation" else "violations"}"
                rule.skipped -> "skipped"
                else -> "not run"
            }
            append("\n  ${"${rule.name}:".padEnd(width + 2)}$result")
        }
    }
}

internal data class KoinModuleVisibility(
    val gradleModule: String,
    val qualifiedName: String,
    val location: String,
    val isPublic: Boolean,
    val isInternal: Boolean,
)

internal fun KoPropertyDeclaration.toKoinModuleVisibility(): KoinModuleVisibility = KoinModuleVisibility(
    gradleModule = moduleName,
    qualifiedName = requireNotNull(fullyQualifiedName),
    location = location,
    isPublic = hasPublicOrDefaultModifier,
    isInternal = hasInternalModifier,
)

internal fun koinModuleVisibilityViolations(declarations: List<KoinModuleVisibility>): List<QualityViolation> =
    declarations.groupBy { it.gradleModule }.flatMap { (gradleModule, modules) ->
        // Android variants and KMP actual declarations can repeat one logical entry point.
        val publicNames = modules.filter { it.isPublic }.map { it.qualifiedName }.distinct().sorted()
        buildList {
            if (publicNames.isEmpty()) {
                add(
                    QualityViolation(
                        location = modules.first().location,
                        reason = "Gradle module '$gradleModule' has no public DI entry point.",
                        suggestion = "Expose one DI module as public; keep supporting DI modules internal.",
                        group = gradleModule,
                    ),
                )
            }
            if (publicNames.size > 1) {
                modules.filter { it.isPublic }.forEach { declaration ->
                    add(
                        QualityViolation(
                            location = declaration.location,
                            reason = "Gradle module '$gradleModule' exposes ${publicNames.size} DI entry points: " +
                                publicNames.joinToString() + ".",
                            suggestion = "Keep one entry point public and mark supporting DI modules internal.",
                            group = gradleModule,
                        ),
                    )
                }
            }
            modules.filter { !it.isPublic }.forEach { declaration ->
                val isEntryPoint = declaration.qualifiedName in publicNames
                if (isEntryPoint || !declaration.isInternal) {
                    add(
                        QualityViolation(
                            location = declaration.location,
                            reason = "DI module '${declaration.qualifiedName}' in '$gradleModule' " +
                                if (isEntryPoint) "has inconsistent visibility across source sets." else "must be internal.",
                            suggestion = if (isEntryPoint) {
                                "Use public visibility consistently for the entry point across source sets."
                            } else {
                                "Mark the supporting DI module internal."
                            },
                            group = gradleModule,
                        ),
                    )
                }
            }
        }
    }
