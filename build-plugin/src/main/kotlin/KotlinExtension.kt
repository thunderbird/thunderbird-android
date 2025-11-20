import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun Project.configureKotlinJavaCompatibility() {
    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(ThunderbirdProjectConfig.Compiler.jvmTarget)
        }
    }
    tasks.withType<JavaCompile>().configureEach {
        sourceCompatibility = ThunderbirdProjectConfig.Compiler.javaCompatibility.toString()
        targetCompatibility = ThunderbirdProjectConfig.Compiler.javaCompatibility.toString()
    }
}
