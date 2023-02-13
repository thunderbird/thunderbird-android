import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByName

val Project.libs: LibrariesForLibs
    get() = extensions.getByName<LibrariesForLibs>("libs")
