package net.thunderbird.gradle.plugin.app.badging

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.Aapt2
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.register

private val variantsToCheck = listOf("release", "beta", "daily")

/**
 * This is a Gradle plugin that adds a task to generate the badging of the APKs and a task to check that the
 * generated badging is the same as the golden badging.
 *
 * This is modified from [nowinandroid](https://github.com/android/nowinandroid) and follows recommendations from
 * [Prevent regressions with CI and badging](https://android-developers.googleblog.com/2023/12/increase-your-apps-availability-across-device-types.html).
 */
class BadgingPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.application")
            }

            configureBadging()
        }
    }

    private fun Project.configureBadging() {
        extensions.configure<ApplicationAndroidComponentsExtension> {
            onVariants { variant ->
                if (variantsToCheck.any { variant.name.contains(it, ignoreCase = true) }) {
                    val capitalizedVariantName = variant.name.capitalized()
                    val generateBadgingTaskName = "generate${capitalizedVariantName}Badging"
                    val generateBadging = tasks.register<GenerateBadgingTask>(generateBadgingTaskName) {
                        apk = variant.artifacts.get(SingleArtifact.APK_FROM_BUNDLE)
                        aapt2Executable = this@configure.sdkComponents.aapt2.flatMap(Aapt2::executable)
                        badging = project.layout.buildDirectory.file(
                            "outputs/apk_from_bundle/${variant.name}/${variant.name}-badging.txt",
                        )
                    }

                    val updateBadgingTaskName = "update${capitalizedVariantName}Badging"
                    tasks.register<Copy>(updateBadgingTaskName) {
                        from(generateBadging.map(GenerateBadgingTask::badging))
                        into(project.layout.projectDirectory.dir("badging"))
                    }

                    val checkBadgingTaskName = "check${capitalizedVariantName}Badging"
                    tasks.register<CheckBadgingTask>(checkBadgingTaskName) {
                        goldenBadging = project.layout.projectDirectory.file("badging/${variant.name}-badging.txt")

                        generatedBadging.set(generateBadging.flatMap(GenerateBadgingTask::badging))

                        this.updateBadgingTaskName = updateBadgingTaskName

                        output = project.layout.buildDirectory.dir("intermediates/$checkBadgingTaskName")
                    }

                    tasks.named("build") {
                        dependsOn(checkBadgingTaskName)
                    }
                }
            }
        }
    }
}

private fun String.capitalized() = replaceFirstChar {
    if (it.isLowerCase()) it.titlecase() else it.toString()
}
