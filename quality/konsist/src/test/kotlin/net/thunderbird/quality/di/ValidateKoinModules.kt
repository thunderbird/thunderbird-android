package net.thunderbird.quality.di

import com.lemonappdev.konsist.api.declaration.KoClassDeclaration
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import com.lemonappdev.konsist.api.declaration.KoPropertyDeclaration
import kotlin.test.Ignore
import kotlin.test.Test
import net.thunderbird.quality.util.QualityViolation
import net.thunderbird.quality.util.assertNoQualityViolations
import net.thunderbird.quality.util.productionScope
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
    fun `di modules outside the root package should be internal`() {
        val violations = productionScope.files.groupBy { it.moduleName }.flatMap { (moduleName, files) ->
            val modules = files.flatMap { it.properties() }.filter { it.isTopLevel && it.name.endsWith("DiModule") }
            if (modules.isEmpty()) return@flatMap emptyList()
            val packages = files.mapNotNull { it.packagee?.name }.distinct()
            val rootPackage = commonProductionPackage(packages)
            val layoutNote = productionPackageLayoutNote(packages)
            if (layoutNote != null) {
                println("Koin package layout [$moduleName]: $layoutNote Visibility was not checked.")
                return@flatMap emptyList()
            }
            modules.mapNotNull { koinModuleVisibilityViolation(it, rootPackage) }
        }.toSet()

        assertNoViolations(
            name = "Visibility",
            message = "DI modules outside the shared production root package must be internal.",
            violations = violations,
        )
    }

    @Test
    fun `koin module files should declare one module`() {
        val violations = productionScope.files
            .map { file -> file to koinModuleDeclarations(file) }
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

    @Test
    fun `koin graph tests should be named after the module they verify`() {
        val declarations = moduleFiles.flatMap { (_, modules) -> modules }
        val violations = testScope.classes()
            .filter { testClass ->
                testClass.containingFile.imports.any {
                    it.name.startsWith("org.koin.test.verify.") ||
                        it.name in setOf("org.koin.core.context.startKoin", "org.koin.core.context.koinApplication")
                }
            }
            .mapNotNull { testClass ->
                val names = koinGraphTestModuleNames(testClass.text)
                if (names.isEmpty()) return@mapNotNull null
                val modules = declarations.filter { it.moduleName == testClass.moduleName && it.name in names }
                    .distinctBy { it.fullyQualifiedName }
                if (names.size != 1 || modules.size != 1) {
                    QualityViolation(
                        location = testClass.location,
                        reason = "Cannot identify a single DI module under test for '${testClass.name}': ${names.sorted()}.",
                        suggestion = "Review the module references before choosing a test name and path.",
                    )
                } else {
                    koinGraphTestNamingViolation(testClass, modules.single())
                }
            }.toSet()

        assertNoViolations(
            name = "Graph-test names",
            message = "Name graph-test classes and files <DiModuleName>Test, and mirror the tested module's package and directory.",
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
            "Visibility" to "di modules outside the root package should be internal",
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
            productionScope.files
                .map { file -> file to file.properties().filter { MODULE_DECLARATION.containsMatchIn(it.value.orEmpty()) } }
                .filter { (_, modules) -> modules.isNotEmpty() }

    }
}

internal fun koinModuleDeclarations(file: KoFileDeclaration): List<KoPropertyDeclaration> =
    file.properties().filter { it.isTopLevel && it.name.endsWith("DiModule") }

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

internal fun commonProductionPackage(packages: List<String>): String? = packages
    .map { it.split('.') }
    .reduceOrNull { prefix, next -> prefix.zip(next).takeWhile { (left, right) -> left == right }.map { it.first } }
    ?.joinToString(".")
    ?.takeIf { it.isNotEmpty() }

internal fun productionPackageLayoutNote(packages: List<String>): String? {
    val rootPackage = commonProductionPackage(packages)
    return when {
        rootPackage == null -> "No shared production package prefix. Review the package layout."
        rootPackage !in packages -> "Derived root '$rootPackage' has no production files directly in it. Review the package layout."
        else -> null
    }
}

internal fun koinModuleVisibilityViolation(
    declaration: KoPropertyDeclaration,
    rootPackage: String?,
): QualityViolation? {
    val packageName = declaration.containingFile.packagee?.name
    if (rootPackage == null || declaration.hasInternalModifier || packageName == rootPackage) return null

    return QualityViolation(
        location = declaration.location,
        reason = "DI module '${declaration.name}' is outside root package '$rootPackage' and must be internal.",
        suggestion = "Mark this supporting DI module internal.",
    )
}

internal fun koinGraphTestModuleNames(text: String): Set<String> {
    val verified = Regex("""\b([a-z]\w*DiModule)\s*\.\s*verify\s*\(""")
        .findAll(text).map { it.groupValues[1] }
    val started = Regex("""\bmodules\s*\(([^)]*)\)""", RegexOption.DOT_MATCHES_ALL)
        .findAll(text).flatMap { call ->
            Regex("""\b[a-z]\w*DiModule\b""").findAll(call.groupValues[1]).map { it.value }
        }
    return (verified + started).toSet()
}

internal fun koinGraphTestNamingViolation(
    testClass: KoClassDeclaration,
    module: KoPropertyDeclaration,
): QualityViolation? {
    val file = testClass.containingFile
    val moduleFile = module.containingFile
    val expectedClass = module.name.replaceFirstChar(Char::uppercase) + "Test"
    val fileName = file.path.substringAfterLast('/')
    val expectedFile = expectedClass + "." + fileName.substringAfter('.')
    val expectedPackage = moduleFile.packagee?.name
    val testDirectory = file.sourceRelativePath.substringBeforeLast('/', "")
    val expectedDirectory = moduleFile.sourceRelativePath.substringBeforeLast('/', "")
    val expectedPath = listOf(expectedDirectory, expectedFile).filter { it.isNotEmpty() }.joinToString("/")
    val corrections = buildList {
        if (testClass.name != expectedClass) add("Class: ${testClass.name} -> $expectedClass")
        if (fileName != expectedFile) add("File: $fileName -> $expectedFile")
        if (file.packagee?.name != expectedPackage) add("Package: ${file.packagee?.name} -> $expectedPackage")
        if (testDirectory != expectedDirectory) add("Path: ${file.sourceRelativePath} -> $expectedPath")
    }
    if (corrections.isEmpty()) return null
    return QualityViolation(
        location = testClass.location,
        reason = "Graph test for '${module.name}' does not match its name or placement.",
        suggestion = corrections.joinToString("; "),
    )
}

private val KoFileDeclaration.sourceRelativePath: String
    get() = path.substringAfter("/src/").split('/').drop(2).joinToString("/")

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
