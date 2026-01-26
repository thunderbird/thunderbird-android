package net.thunderbird.gradle.plugin

import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByName

val Project.libs
    get(): LibrariesForLibs = extensions.getByName<LibrariesForLibs>("libs")
