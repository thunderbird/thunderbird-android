package net.thunderbird.gradle.plugin

import org.gradle.api.JavaVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

object ProjectConfig {

    object Compiler {
        val javaCompatibility = JavaVersion.VERSION_17
        val jvmTarget = JvmTarget.JVM_17
    }
}
