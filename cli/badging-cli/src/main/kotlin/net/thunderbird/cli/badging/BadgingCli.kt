package net.thunderbird.cli.badging

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.enum
import java.io.File
import kotlin.system.exitProcess
import net.thunderbird.cli.badging.logger.ColoredConsoleLogSink
import net.thunderbird.core.logging.DefaultLogger
import net.thunderbird.core.logging.LogLevel
import net.thunderbird.core.logging.Logger

class BadgingCli : CliktCommand(name = "badging") {
    private lateinit var logger: Logger
    private lateinit var sdkResolver: AndroidSdkResolver
    private lateinit var badgingGenerator: BadgingGenerator
    private lateinit var badgingValidator: BadgingValidator
    private lateinit var badgingUpdater: BadgingUpdater

    private val module: String by option(help = "Gradle module, e.g., app-thunderbird").required()

    private val flavor: ProductFlavor by option(
        help = "Product flavor (e.g., full or foss)",
    ).enum<ProductFlavor> { it.name.lowercase() }
        .required()

    private val buildType: BuildType by option(help = "Build type (e.g release, beta, daily, debug)")
        .enum<BuildType> { it.name.lowercase() }
        .required()

    private val aapt2Path: String? by option(help = "Optional path to aapt2; otherwise auto-detected from Android SDK")
    private val bundletoolPath: String? by option(
        help = "Path to bundletool (binary or .jar) used to extract APKs from AAB",
    )
    private val sdkRoot: String? by option(help = "Override SDK root detection (ANDROID_HOME/ANDROID_SDK_ROOT)")

    private val build: Boolean by option(
        help = "If set, will run Gradle to build the apk",
    ).flag(default = false)
    private val outputDir: String? by option(help = "Output directory for golden badging (default: <module>/badging)")

    private val update: Boolean by option(
        help = "Overwrite the golden badging with current badging (disables validation for this run)",
    ).flag(default = false)

    private val logLevel: LogLevel by option(
        help = "Log level (e.g. verbose, debug, info, warn, error) (default: info)",
    ).enum<LogLevel> { it.name.lowercase() }
        .default(LogLevel.INFO)

    override fun help(context: Context): String = "Generate and validate Android badging for an APK"

    override fun run() {
        @OptIn(kotlin.time.ExperimentalTime::class)
        logger = DefaultLogger(ColoredConsoleLogSink(level = logLevel))
        sdkResolver = AndroidSdkResolver(logger)
        badgingGenerator = BadgingGenerator(logger, CommandExecutor(logger))
        badgingValidator = BadgingValidator(logger)
        badgingUpdater = BadgingUpdater(logger)

        val workingDir = File(System.getProperty("user.dir"))
        val moduleDir = ensureModuleDirectory(workingDir, module)
        val buildDir = ensureBuildDir(moduleDir)
        val badgingDir = ensureBadgingDirectory(outputDir, moduleDir)

        val aapt2 = when (val result = sdkResolver.resolveAapt2(sdkRoot, aapt2Path)) {
            is ResolverResult.Success -> result.file
            is ResolverResult.Failure -> {
                logger.error { "Failed to resolve aapt2: ${result.message}" }
                exitProcess(1)
            }
        }
        val bundleTool = when (val result = sdkResolver.resolveBundleTool(bundletoolPath)) {
            is ResolverResult.Success -> result.file
            is ResolverResult.Failure -> {
                logger.error { "Failed to resolve bundletool: ${result.message}" }
                exitProcess(1)
            }
        }

        val fileName = "${flavor}${buildType.toCamelCase()}-badging.txt"
        val badgingFile = File(buildDir, fileName)
        val goldenBadgingFile = File(badgingDir, fileName)
        val actualBadging = badgingGenerator.generateBadging(
            workingDir = workingDir,
            moduleDir = moduleDir,
            flavor = flavor,
            buildType = buildType,
            aapt2 = aapt2,
            bundleTool = bundleTool,
            shouldBuild = build,
        )

        saveBadging(
            badgingFile = badgingFile,
            badging = actualBadging,
        )

        if (update) {
            logger.info { "--update specified: skipping check and updating golden badging." }
            badgingUpdater.update(
                goldenBadgingFile = goldenBadgingFile,
                actualBadging = actualBadging,
            )
        } else {
            badgingValidator.validate(
                goldenBadgingFile = goldenBadgingFile,
                actualBadging = actualBadging,
            )
        }
    }

    private fun saveBadging(
        badgingFile: File,
        badging: String,
    ): File {
        badgingFile.writeText(badging + "\n")
        logger.info(tag = "badging") { "Current badging written to: ${badgingFile.absolutePath}" }
        return badgingFile
    }

    private fun ensureModuleDirectory(workingDir: File, path: String): File {
        val moduleDir = File(workingDir, path)
        if (!moduleDir.exists() || !moduleDir.isDirectory) {
            error("Module directory does not exist or is not a directory: ${moduleDir.absolutePath}")
        } else {
            return moduleDir
        }
    }

    private fun ensureBuildDir(moduleDir: File): File {
        val tempDir = File(moduleDir, "build/outputs/badging")
        if (!tempDir.exists()) {
            val created = tempDir.mkdirs()
            if (!created) {
                error("Failed to create temporary directory: ${tempDir.absolutePath}")
            }
        } else if (!tempDir.isDirectory) {
            error("Temporary path is not a directory: ${tempDir.absolutePath}")
        }
        return tempDir
    }

    private fun ensureBadgingDirectory(outputPath: String?, moduleDir: File): File {
        val badging = outputPath?.let { File(it) } ?: File(moduleDir, "badging")
        if (!badging.exists()) {
            val created = badging.mkdirs()
            if (!created) {
                error("Failed to create output directory: ${badging.absolutePath}")
            }
        } else if (!badging.isDirectory) {
            error("Output path is not a directory: ${badging.absolutePath}")
        }

        return badging
    }
}
