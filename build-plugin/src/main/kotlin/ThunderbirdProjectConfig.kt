import org.gradle.api.JavaVersion

object ThunderbirdProjectConfig {

    val javaVersion = JavaVersion.VERSION_11

    const val androidSdkMin = 21
    const val androidSdkTarget = 31
    const val androidSdkCompile = 33
}
