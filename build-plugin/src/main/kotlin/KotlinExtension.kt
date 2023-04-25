import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun Project.configureKotlinJavaCompatibility() {
    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = ThunderbirdProjectConfig.javaCompatibilityVersion.toString()
        }
    }
}
