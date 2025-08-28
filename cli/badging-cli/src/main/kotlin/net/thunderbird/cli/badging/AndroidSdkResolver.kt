package net.thunderbird.cli.badging

import java.io.File
import java.net.URI
import kotlin.system.exitProcess
import net.thunderbird.core.logging.Logger

const val BUNDLE_TOOL_VERSION = "1.18.1"

sealed class ResolverResult {
    data class Success(val file: File) : ResolverResult()
    data class Failure(val message: String) : ResolverResult()
}

class AndroidSdkResolver(
    private val logger: Logger,
) {

    fun resolveAapt2(sdkDir: String?, aapt2Path: String?): ResolverResult {
        val sdkDir = resolveSdkRoot(sdkDir)
        if (aapt2Path != null) {
            val aapt2 = File(aapt2Path)
            return checkAapt2(aapt2)
        }

        val aapt2 = findAapt2(sdkDir)
        return if (aapt2 == null) {
            ResolverResult.Failure("aapt2 not found in SDK under $sdkDir")
        } else {
            checkAapt2(aapt2)
        }
    }

    private fun checkAapt2(aapt2: File): ResolverResult {
        return if (!aapt2.exists()) {
            ResolverResult.Failure("aapt2 not found at ${aapt2.absolutePath}")
        } else if (!aapt2.canExecute()) {
            ResolverResult.Failure("aapt2 not executable at ${aapt2.absolutePath}")
        } else {
            logger.debug { "Using aapt2 at: ${aapt2.absolutePath}" }
            ResolverResult.Success(aapt2)
        }
    }

    private fun findAapt2(sdkDir: String): File? {
        val buildToolsDir = resolveBuildToolsDir(sdkDir)

        val aapt2 = File(buildToolsDir, "aapt2")
        val aapt2Exe = File(buildToolsDir, "aapt2.exe")

        return when {
            aapt2Exe.exists() -> aapt2Exe
            aapt2.exists() -> aapt2
            else -> null
        }
    }

    fun resolveBundleTool(bundleToolPath: String?): ResolverResult {
        if (bundleToolPath != null) {
            val bundleTool = File(bundleToolPath)
            return checkBundleTool(bundleTool)
        }

        val cacheDir = File(System.getProperty("user.home"), ".cache/thunderbird-cli")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val bundleTool = File(cacheDir, "bundletool-all-${BUNDLE_TOOL_VERSION}.jar")
        val result = checkBundleTool(bundleTool)
        return if (result is ResolverResult.Success) {
            result
        } else {
            logger.info {
                "bundletool $BUNDLE_TOOL_VERSION not found in cache, will download to ${bundleTool.absolutePath}"
            }
            downloadBundleTool(bundleTool, BUNDLE_TOOL_VERSION)
            if (!bundleTool.exists() || bundleTool.length() == 0L) {
                ResolverResult.Failure("Failed to download bundletool to ${bundleTool.absolutePath}")
            }
            checkBundleTool(bundleTool)
        }
    }

    private fun checkBundleTool(bundleTool: File): ResolverResult {
        return if (!bundleTool.exists()) {
            ResolverResult.Failure("bundletool not found at ${bundleTool.absolutePath}")
        } else if (!bundleTool.canRead()) {
            ResolverResult.Failure("bundletool not readable at ${bundleTool.absolutePath}")
        } else {
            logger.debug { "Using bundletool at: ${bundleTool.absolutePath}" }
            ResolverResult.Success(bundleTool)
        }
    }

    private fun downloadBundleTool(target: File, version: String) {
        val url = URI.create(
            "https://github.com/google/bundletool/releases/download/$version/bundletool-all-$version.jar",
        )
        // no direct println here; caller gets a log above
        target.outputStream().use { out ->
            url.toURL().openStream().use { input ->
                input.copyTo(out)
            }
        }
    }

    private fun resolveSdkRoot(sdkRoot: String?): String {
        return sdkRoot
            ?: System.getenv("ANDROID_HOME")
            ?: System.getenv("ANDROID_SDK_ROOT")
            ?: error("ANDROID_HOME or ANDROID_SDK_ROOT must be set or pass --sdk-root")
    }

    private fun resolveBuildToolsDir(sdkDir: String): File {
        val buildToolsDir = File("$sdkDir/build-tools")
        if (!buildToolsDir.exists() || !buildToolsDir.isDirectory) {
            logger.error { "build-tools directory not found under SDK ($sdkDir)" }
            exitProcess(1)
        }

        return findLatestBuildToolsDir(buildToolsDir)
    }

    private fun findLatestBuildToolsDir(buildToolsDir: File): File {
        val buildToolsDir = buildToolsDir.listFiles()
            ?.filter { it.isDirectory }
            ?.maxWithOrNull(
                Comparator<File> { a, b ->
                    fun parse(s: String): List<Int> = s.split('.').map { it.toIntOrNull() ?: -1 }
                    val pa = parse(a.name)
                    val pb = parse(b.name)
                    val maxLen = maxOf(pa.size, pb.size)
                    for (i in 0 until maxLen) {
                        val va = pa.getOrElse(i) { 0 }
                        val vb = pb.getOrElse(i) { 0 }
                        if (va != vb) return@Comparator va.compareTo(vb)
                    }
                    0
                },
            )

        if (buildToolsDir == null) {
            logger.error { "No build-tools versions found in $buildToolsDir" }
            exitProcess(1)
        } else {
            logger.debug { "Using build-tools version: ${buildToolsDir.name}" }
            return buildToolsDir
        }
    }
}
