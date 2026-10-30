package net.thunderbird.gradle.plugin.app.baselineprofile

import java.util.Properties
import net.thunderbird.gradle.plugin.ProjectConfig
import net.thunderbird.gradle.plugin.extension.android
import net.thunderbird.gradle.plugin.extension.androidComponents
import net.thunderbird.gradle.plugin.extension.baselineProfileForProducer
import net.thunderbird.gradle.plugin.extension.kotlin
import net.thunderbird.gradle.plugin.libs
import net.thunderbird.gradle.plugin.quality.detekt.DetektPlugin
import net.thunderbird.gradle.plugin.quality.spotless.SpotlessPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Convention plugin for the Baseline Profile producer module.
 *
 * Applies the Android test and Baseline Profile plugins together with code quality plugins, and configures:
 * - Benchmark account credentials, read from `local.properties` and passed as instrumentation runner arguments.
 *   Instrumentation arguments are not secret: the password is visible in the `am instrument` command line on the
 *   device and in Gradle `--info`/`--debug` logs. Only use a throwaway account, never a personal one.
 * - Profile generation on connected devices instead of Gradle managed devices.
 * - The `targetAppId` instrumentation argument, resolved from the tested APK of each variant.
 */
// suppressed unused since this is used by the Gradle build system.
@Suppress("Unused")
abstract class BaselineProfileProducerAppPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        with(pluginManager) {
            apply(libs.plugins.android.test.get().pluginId)
            apply(libs.plugins.androidx.baselineprofile.get().pluginId)
            apply(DetektPlugin::class.java)
            apply(SpotlessPlugin::class.java)
        }

        android {
            compileSdk = ProjectConfig.Android.sdkCompile

            defaultConfig {
                // Baseline Profile collection requires API 28+ (rooted) or API 33+ (non-rooted).
                minSdk = 28
                targetSdk = ProjectConfig.Android.sdkTarget

                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

                // Account used by the benchmarks, see AccountSetup.kt in :library:baselineprofile-common.
                // K-9 Mail has no build type with the demo backend, so a real account is required.
                @Suppress("UnstableApiUsage")
                val localPropertiesFile = isolated.rootProject.projectDirectory.file("local.properties").asFile
                val localProperties = Properties().apply {
                    if (localPropertiesFile.exists()) {
                        localPropertiesFile.reader().use { load(it) }
                    }
                }
                mapOf(
                    "tfa.benchmark.account.email" to "accountEmail",
                    "tfa.benchmark.account.password" to "accountPassword",
                    "tfa.benchmark.account.display_name" to "accountDisplayName",
                    "tfa.benchmark.account.incoming_server" to "accountIncomingServer",
                    "tfa.benchmark.account.outgoing_server" to "accountOutgoingServer",
                ).forEach { (propertyKey, argumentKey) ->
                    localProperties.getProperty(propertyKey)
                        ?.let { testInstrumentationRunnerArguments[argumentKey] = it }
                }
            }

            compileOptions {
                sourceCompatibility = ProjectConfig.Compiler.javaCompatibility
                targetCompatibility = ProjectConfig.Compiler.javaCompatibility
            }

            flavorDimensions += listOf("app")
            productFlavors {
                create("foss") { dimension = "app" }
                create("full") { dimension = "app" }
            }

            packaging {
                resources {
                    excludes += listOf(
                        "/META-INF/{AL2.0,LGPL2.1}",
                        "/META-INF/DEPENDENCIES",
                        "/META-INF/LICENSE",
                        "/META-INF/LICENSE.txt",
                        "/META-INF/NOTICE",
                        "/META-INF/NOTICE.txt",
                        "/META-INF/README",
                        "/META-INF/README.md",
                        "/META-INF/CHANGES",
                        "/LICENSE.txt",
                    )
                }
            }
            @Suppress("UnstableApiUsage")
            experimentalProperties["android.experimental.self-instrumenting"] = true
        }

        kotlin {
            compilerOptions {
                jvmTarget.set(ProjectConfig.Compiler.jvmTarget)
            }
        }

        baselineProfileForProducer {
            // Run the generators and benchmarks on a connected device instead of a Gradle managed device.
            useConnectedDevices = true
        }

        androidComponents {
            onVariants { variant ->
                val artifactsLoader = variant.artifacts.getBuiltArtifactsLoader()
                @Suppress("UnstableApiUsage")
                variant.instrumentationRunnerArguments.put(
                    "targetAppId",
                    variant.testedApks.map { artifactsLoader.load(it)?.applicationId },
                )
            }
        }
    }
}
