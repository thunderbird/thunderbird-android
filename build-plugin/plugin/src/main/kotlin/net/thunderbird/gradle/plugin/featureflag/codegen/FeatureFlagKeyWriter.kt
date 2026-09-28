package net.thunderbird.gradle.plugin.featureflag.codegen

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import java.io.File
import net.thunderbird.gradle.plugin.featureflag.FeatureFlagCatalog
import net.thunderbird.gradle.plugin.featureflag.FlagRegistry

/**
 * Generates Kotlin source code for a feature flag key enumeration.
 *
 * @param packageName The package name for the generated enum class
 * @param enumName The name of the generated enum class
 */
internal class FeatureFlagKeyWriter(
    private val packageName: String,
    private val enumName: String,
) {
    /**
     * Generates and writes a Kotlin enum file containing feature flag keys from the provided catalog.
     *
     * @param catalog The feature flag catalog containing the flag definitions to generate enum constants from
     * @param outputDir The directory where the generated Kotlin source file will be written
     */
    fun write(catalog: FeatureFlagCatalog, outputDir: File) {
        val typeSpec = createEnumSpec(
            enumName = enumName,
            flagRegistries = catalog.flags,
        )
        FileSpec.builder(packageName = packageName, fileName = enumName)
            .addFileComment("!! GENERATED FILE - DO NOT CHANGE! CHANGES ARE GOING TO BE OVERWRITTEN. !!")
            .addType(typeSpec)
            .build()
            .writeTo(outputDir)
    }

    private fun createEnumSpec(
        enumName: String,
        flagRegistries: List<FlagRegistry>,
    ): TypeSpec = TypeSpec
        .enumBuilder(enumName)
        .apply {
            addSuperinterfaces(
                listOf(
                    ClassName(packageName = "net.thunderbird.core.featureflag", "FeatureFlagKey"),
                ),
            )
            primaryConstructor(
                primaryConstructor = FunSpec
                    .constructorBuilder()
                    .addParameter("key", String::class)
                    .addParameter("description", String::class.asTypeName().copy(nullable = true))
                    .build(),
            )
            addProperty(
                PropertySpec.builder("key", String::class)
                    .initializer("key")
                    .addModifiers(KModifier.OVERRIDE)
                    .build(),
            )
            addProperty(
                PropertySpec
                    .builder(
                        name = "description",
                        type = String::class.asTypeName().copy(nullable = true),
                    )
                    .initializer("description")
                    .addModifiers(KModifier.OVERRIDE)
                    .build(),
            )
            flagRegistries.forEach { registry ->
                addEnumConstant(
                    name = registry.key.uppercase(),
                    typeSpec = TypeSpec
                        .anonymousClassBuilder()
                        .addSuperclassConstructorParameter(
                            CodeBlock.of(
                                "%S, %L",
                                registry.key,
                                registry.description?.let { description -> CodeBlock.of("%S", description) },
                            ),
                        )
                        .build(),
                )
            }
        }
        .build()
}
