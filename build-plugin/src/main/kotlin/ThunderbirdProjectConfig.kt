import org.gradle.api.JavaVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

object ThunderbirdProjectConfig {

    object Android {
        const val sdkMin = 23

        // Only needed for application
        const val sdkTarget = 35
        const val sdkCompile = 36
    }

    object Compiler {
        val javaCompatibility = JavaVersion.VERSION_11
        val jvmTarget = JvmTarget.JVM_11
    }
}
