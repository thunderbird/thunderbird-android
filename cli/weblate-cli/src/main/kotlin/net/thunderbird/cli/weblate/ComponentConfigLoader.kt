package net.thunderbird.cli.weblate

import java.io.File
import kotlinx.serialization.json.Json
import net.thunderbird.cli.weblate.api.ComponentConfig

class ComponentConfigLoader {
    private val json = Json { ignoreUnknownKeys = true }

    fun load(file: File): ComponentConfig {
        val text = file.readText(Charsets.UTF_8)
        return json.decodeFromString(ComponentConfig.serializer(), text)
    }
}
