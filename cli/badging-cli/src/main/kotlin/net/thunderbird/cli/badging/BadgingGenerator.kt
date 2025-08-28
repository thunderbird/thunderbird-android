package net.thunderbird.cli.badging

import java.io.File
import kotlin.system.exitProcess
import net.thunderbird.core.logging.Logger

class BadgingGenerator(
    private val logger: Logger,
    private val executor: CommandExecutor,
) {

    fun generateBadging(
        workingDir: File,
        moduleDir: File,
        flavor: ProductFlavor,
        buildType: BuildType,
        aapt2: File,
        bundleTool: File,
        shouldBuild: Boolean,
    ): String {
        val bundleFile = resolveBundle(workingDir, moduleDir, flavor, buildType, shouldBuild)
        if (bundleFile == null) {
            logger.info {
                "No bundle found for $flavor/$buildType and --build not specified. Skipping badging generation."
            }
            exitProcess(0)
        }

        val apkFile = resolveApkFromBundle(moduleDir, flavor, buildType, bundleFile, bundleTool, shouldBuild)
        val aapt2Result = executor.execAndCapture(
            listOf(aapt2.absolutePath, "dump", "badging", apkFile.absolutePath),
            workingDir,
        )

        return if (aapt2Result.exitCode != 0) {
            logger.error { "aapt2 failed with exit ${aapt2Result.exitCode}" }
            exitProcess(1)
        } else if (aapt2Result.output.isBlank()) {
            val hint = if (aapt2Result.error.isNotBlank()) " aapt2 stderr: ${aapt2Result.error.trim()}" else ""
            logger.error { "aapt2 produced empty badging output for ${apkFile.name}.$hint" }
            exitProcess(1)
        } else {
            normalizeBadging(aapt2Result.output)
        }
    }

    private fun resolveBundle(
        workingDir: File,
        moduleDir: File,
        flavor: ProductFlavor,
        buildType: BuildType,
        shouldBuild: Boolean,
    ): File? {
        val bundleFile = if (shouldBuild) {
            createBundle(workingDir, moduleDir, flavor, buildType)
        } else {
            findBundle(moduleDir, flavor, buildType)
        }

        return if (bundleFile != null && bundleFile.exists()) {
            logger.info { "Using bundle at: ${bundleFile.absolutePath}" }
            bundleFile
        } else {
            null
        }
    }

    private fun resolveApkFromBundle(
        moduleDir: File,
        flavor: ProductFlavor,
        buildType: BuildType,
        bundleFile: File,
        bundleTool: File,
        shouldBuild: Boolean,
    ): File {
        val flavorBuildType = "${flavor}${buildType.toCamelCase()}"
        val outputDir = File(moduleDir, "build/outputs/apk_from_bundle/$flavorBuildType")
        if (!outputDir.exists()) {
            if (!outputDir.mkdirs()) {
                logger.error { "Failed to create output dir at: ${outputDir.absolutePath}" }
                exitProcess(1)
            }
        }

        val apk = File(outputDir, "${moduleDir.name}-$flavor-$buildType-badging.apk")

        if (!shouldBuild && apk.exists() && apk.length() > 0L) {
            logger.info { "Using APK at: ${apk.absolutePath}" }
            return apk
        }

        extractApkFromBundle(
            moduleDir = moduleDir,
            outputDir = outputDir,
            bundleTool = bundleTool,
            bundleFile = bundleFile,
            apk = apk,
        )

        logger.info { "Using APK at: ${apk.absolutePath}" }
        return apk
    }

    private fun extractApkFromBundle(
        moduleDir: File,
        outputDir: File,
        bundleTool: File,
        bundleFile: File,
        apk: File,
    ) {
        val apksFile = File(outputDir, "badging.apks")
        if (apksFile.exists()) apksFile.delete()

        val isJar = bundleTool.extension.equals("jar", ignoreCase = true)
        val cmd = if (isJar) {
            listOf(
                "java",
                "-jar",
                bundleTool.absolutePath,
                "build-apks",
                "--bundle=${bundleFile.absolutePath}",
                "--output=${apksFile.absolutePath}",
                "--mode=universal",
            )
        } else {
            listOf(
                bundleTool.absolutePath,
                "build-apks",
                "--bundle=${bundleFile.absolutePath}",
                "--output=${apksFile.absolutePath}",
                "--mode=universal",
            )
        }

        val result = executor.exec(cmd, moduleDir)
        if (result.exitCode != 0) {
            logger.error { "bundletool build-apks failed with exit ${result.exitCode}" }
            exitProcess(1)
        }

        val unzipDir = File(outputDir, "unzip")
        if (unzipDir.exists()) unzipDir.deleteRecursively()
        unzipDir.mkdirs()
        val unzipCmd = listOf("unzip", "-o", apksFile.absolutePath, "-d", unzipDir.absolutePath)
        val unzipResult = executor.exec(unzipCmd, moduleDir)
        if (unzipResult.exitCode != 0) {
            logger.error { "Failed to unzip .apks with exit ${unzipResult.exitCode}" }
            exitProcess(1)
        }

        val universalApk = File(unzipDir, "universal.apk")
        if (!universalApk.exists() || universalApk.length() == 0L) {
            logger.error { "universal.apk not found or empty in generated .apks at: ${universalApk.absolutePath}" }
            exitProcess(1)
        }

        universalApk.copyTo(apk, overwrite = true)
    }

    private fun normalizeBadging(input: String): String {
        return input.lineSequence()
            .map { line ->
                if (line.startsWith("package:")) {
                    line
                        .replace(Regex("versionName='[^']*'"), "")
                        .replace(Regex("versionCode='[^']*'"), "")
                        .replace(Regex("\\s+"), " ")
                        .trim()
                } else if (line.trim().startsWith("uses-feature-not-required:")) {
                    line.trim()
                } else {
                    line
                }
            }
            .filter { it.isNotBlank() }
            .sorted()
            .joinToString("\n") + "\n"
    }

    private fun findBundle(moduleDir: File, flavor: ProductFlavor, buildType: BuildType): File? {
        val bundleDir = File(moduleDir, "build/outputs/bundle/${generateFolderName(flavor, buildType)}")
        return bundleDir.listFiles()?.firstOrNull { it.extension == "aab" }
    }

    private fun createBundle(
        workingDir: File,
        moduleDir: File,
        flavor: ProductFlavor,
        buildType: BuildType,
    ): File? {
        val variantName = generateVariantName(flavor, buildType)
        val result = executor.exec(listOf("./gradlew", "${moduleDir.name}:bundle$variantName"), workingDir)
        if (result.exitCode != 0) {
            logger.error { "Gradle bundle task failed with exit ${result.exitCode}" }
            exitProcess(1)
        }
        return findBundle(moduleDir, flavor, buildType)
    }

    private fun generateVariantName(flavor: ProductFlavor, buildType: BuildType): String {
        return flavor.toCamelCase() + buildType.toCamelCase()
    }

    private fun generateFolderName(flavor: ProductFlavor, buildType: BuildType): String {
        return "${flavor}${buildType.toCamelCase()}"
    }
}
