import com.android.build.api.dsl.ApkSigningConfig
import java.io.FileInputStream
import java.util.Properties
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project

private const val SIGNING_FOLDER = ".signing"
private const val SIGNING_FILE_ENDING = ".signing.properties"
private const val UPLOAD_FILE_ENDING = ".upload.properties"

private const val PROPERTY_STORE_FILE = "storeFile"
private const val PROPERTY_STORE_PASSWORD = "storePassword"
private const val PROPERTY_KEY_ALIAS = "keyAlias"
private const val PROPERTY_KEY_PASSWORD = "keyPassword"

/**
 * Creates an [ApkSigningConfig] for the given signing type.
 *
 * The signing properties are read from a file in the `.signing` folder in the project root directory.
 * File names are expected to be in the format `$app.$type.signing.properties` or `$app.$type.upload.properties`.
 *
 * The file should contain the following properties:
 * - `$app.$type.storeFile`
 * - `$app.$type.storePassword`
 * - `$app.$type.keyAlias`
 * - `$app.$type.keyPassword`
 *
 * @param project the project to create the signing config for
 * @param signingType the signing type to create the signing config for
 * @param isUpload whether the upload or signing config is used
 */
fun NamedDomainObjectContainer<out ApkSigningConfig>.createSigningConfig(
    project: Project,
    signingType: SigningType,
    isUpload: Boolean = true,
) {
    val properties = project.readSigningProperties(signingType, isUpload)

    if (properties.hasSigningConfig(signingType)) {
        create(signingType.type) {
            storeFile = project.file(properties.getSigningProperty(signingType, PROPERTY_STORE_FILE))
            storePassword = properties.getSigningProperty(signingType, PROPERTY_STORE_PASSWORD)
            keyAlias = properties.getSigningProperty(signingType, PROPERTY_KEY_ALIAS)
            keyPassword = properties.getSigningProperty(signingType, PROPERTY_KEY_PASSWORD)
        }
    } else {
        project.logger.warn("Signing config not created for ${signingType.type}")
    }
}

/**
 * Returns the [ApkSigningConfig] for the given signing type.
 *
 * @param signingType the signing type to get the signing config for
 */
fun NamedDomainObjectContainer<out ApkSigningConfig>.getByType(signingType: SigningType): ApkSigningConfig? {
    return findByName(signingType.type)
}

private fun Project.readSigningProperties(signingType: SigningType, isUpload: Boolean) = Properties().apply {
    val signingPropertiesFile = if (isUpload) {
        rootProject.file("$SIGNING_FOLDER/${signingType.id}$UPLOAD_FILE_ENDING")
    } else {
        rootProject.file("$SIGNING_FOLDER/${signingType.id}$SIGNING_FILE_ENDING")
    }

    if (signingPropertiesFile.exists()) {
        FileInputStream(signingPropertiesFile).use { inputStream ->
            load(inputStream)
        }
    } else {
        logger.warn("Signing properties file not found: $signingPropertiesFile")
    }
}

private fun Properties.hasSigningConfig(signingType: SigningType): Boolean {
    return isNotEmpty() &&
        containsKey(signingType, PROPERTY_STORE_FILE) &&
        containsKey(signingType, PROPERTY_STORE_PASSWORD) &&
        containsKey(signingType, PROPERTY_KEY_ALIAS) &&
        containsKey(signingType, PROPERTY_KEY_PASSWORD)
}

private fun Properties.containsKey(signingType: SigningType, key: String): Boolean {
    return containsKey("${signingType.id}.$key")
}

private fun Properties.getSigningProperty(signingType: SigningType, key: String): String {
    return getProperty("${signingType.id}.$key")
        ?: throw IllegalArgumentException("Missing property: ${signingType.type}.$key")
}
