pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }

    versionCatalogs.create("libs") {
        from(files("../gradle/libs.versions.toml"))
    }
}

rootProject.name = "build-plugin"
