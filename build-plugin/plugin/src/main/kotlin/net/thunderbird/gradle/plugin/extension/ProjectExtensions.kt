package net.thunderbird.gradle.plugin.extension

import androidx.baselineprofile.gradle.consumer.BaselineProfileConsumerExtension
import androidx.baselineprofile.gradle.producer.BaselineProfileProducerExtension
import com.android.build.api.dsl.TestExtension
import com.android.build.api.variant.TestAndroidComponentsExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/**
 * Configures the [TestExtension] of this project.
 */
internal fun Project.android(block: TestExtension.() -> Unit) {
    extensions.configure<TestExtension>(block)
}

/**
 * Configures the [KotlinAndroidProjectExtension] of this project.
 */
internal fun Project.kotlin(block: KotlinAndroidProjectExtension.() -> Unit) {
    extensions.configure<KotlinAndroidProjectExtension>(block)
}

/**
 * Configures the [BaselineProfileProducerExtension] of this project.
 */
internal fun Project.baselineProfileForProducer(block: BaselineProfileProducerExtension.() -> Unit) {
    extensions.configure<BaselineProfileProducerExtension>(block)
}

/**
 * Configures the [BaselineProfileConsumerExtension] of this project.
 */
internal fun Project.baselineProfileForConsumer(block: BaselineProfileConsumerExtension.() -> Unit) {
    extensions.configure<BaselineProfileConsumerExtension>(block)
}

/**
 * Configures the [TestAndroidComponentsExtension] of this project.
 */
internal fun Project.androidComponents(block: TestAndroidComponentsExtension.() -> Unit) {
    extensions.configure<TestAndroidComponentsExtension>(block)
}
