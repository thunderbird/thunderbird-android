package net.thunderbird.cli.weblate.command

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.mordant.terminal.YesNoPrompt
import net.thunderbird.cli.weblate.api.Component
import net.thunderbird.cli.weblate.api.ComponentConfig
import net.thunderbird.cli.weblate.api.ComponentCreate
import net.thunderbird.cli.weblate.api.ComponentInfo
import net.thunderbird.cli.weblate.api.WeblateClient
import net.thunderbird.cli.weblate.api.WeblateConfig
import net.thunderbird.cli.weblate.project.ModuleDiscovery
import net.thunderbird.cli.weblate.project.ModuleInfo
import net.thunderbird.cli.weblate.project.ResourceType

class CreateComponent : BaseCommand(name = "create") {

    override fun help(context: Context): String = "Create missing components"

    override fun onRun(
        client: WeblateClient,
        defaultComponentConfig: ComponentConfig,
        managedComponents: Set<String>,
    ) {
        val allComponents = client.loadComponents()
        val apiConfig = WeblateConfig()
        val defaultComponent = allComponents.find { it.info.slug == apiConfig.defaultComponent }
        if (defaultComponent == null) {
            println("    ❌ Could not find default component: ${apiConfig.defaultComponent}")
            return
        }

        val weblateSlugs = allComponents.map { it.info.slug }.toSet()
        val localModules = ModuleDiscovery().discoverLocalModules()

        println("Found ${localModules.size} local modules with Android or Compose strings")

        val missingInWeblate = localModules.filter { it.slug !in weblateSlugs }

        println()
        if (missingInWeblate.isNotEmpty()) {
            println("Modules missing in Weblate:")
            missingInWeblate.forEach {
                createComponentFromModule(
                    client = client,
                    module = it,
                    defaultComponent = defaultComponent,
                    defaultComponentConfig = defaultComponentConfig,
                )
            }
        } else {
            println("All local modules with resource strings have a corresponding component in Weblate.")
        }

        reportUnmanagedManagedComponents(localModules, managedComponents)
    }

    private fun createComponentFromModule(
        client: WeblateClient,
        module: ModuleInfo,
        defaultComponent: Component,
        defaultComponentConfig: ComponentConfig,
    ) {
        println()
        println("  - ${module.path} (type: ${module.type})")
        println("    expected name: \"${module.name}\"")
        println("    expected slug: \"${module.slug}\"")

        if (config.dryRun) {
            println("    (Dry run: would create component)")
        } else {
            val createPayload = createComponentPayload(
                module = module,
                defaultConfig = defaultComponentConfig,
                defaultInfo = defaultComponent.info,
            )
            if (YesNoPrompt("    Do you want to create this component?", terminal).ask() == true) {
                println("    Creating component...")
                val success = executeCreateComponent(
                    client = client,
                    component = createPayload,
                )
                if (!success) {
                    println("    Stopping execution due to failure.")
                    return
                }
            } else {
                println("    Skipped.")
            }
        }
    }

    private fun createComponentPayload(
        module: ModuleInfo,
        defaultConfig: ComponentConfig,
        defaultInfo: ComponentInfo,
    ): ComponentCreate {
        val apiConfig = WeblateConfig()
        val (fileMask, template) = when (module.type) {
            ResourceType.ANDROID -> {
                "${module.path}/src/main/res/values-*/strings.xml" to
                    "${module.path}/src/main/res/values/strings.xml"
            }

            ResourceType.COMPOSE -> {
                "${module.path}/src/commonMain/composeResources/values-*/strings.xml" to
                    "${module.path}/src/commonMain/composeResources/values/strings.xml"
            }
        }

        return ComponentCreate(
            name = module.name,
            slug = module.slug,
            project = "${apiConfig.baseUrl}projects/${apiConfig.projectName}/",
            fileMask = fileMask,
            template = template,
            fileFormat = if (module.type == ResourceType.ANDROID) "aresource" else "cmp-resource",
            category = defaultInfo.category,
            linkedComponent = defaultInfo.url,
            repo = "weblate://thunderbird/thunderbird-android/app-common",
            vcs = "github",
            config = defaultConfig,
        )
    }

    @Suppress("TooGenericExceptionCaught")
    private fun executeCreateComponent(
        client: WeblateClient,
        component: ComponentCreate,
    ): Boolean {
        return try {
            val success = client.createComponent(component)
            if (success) {
                println("    ✅ Created component successfully")
            } else {
                println("    ❌ Failed to create component")
            }
            success
        } catch (e: Exception) {
            println("    ❌ Error creating component: ${e.message}")
            false
        }
    }
}
