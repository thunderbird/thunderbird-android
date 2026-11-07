import net.thunderbird.gradle.plugin.ProjectConfig
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun Project.configureKotlinJavaCompatibility() {
    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(ProjectConfig.Compiler.jvmTarget)
        }
    }
    tasks.withType<JavaCompile>().configureEach {
        sourceCompatibility = ProjectConfig.Compiler.javaCompatibility.toString()
        targetCompatibility = ProjectConfig.Compiler.javaCompatibility.toString()
    }
}
