import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.kotlin.dsl.named
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

val NamedDomainObjectContainer<KotlinSourceSet>.androidHostTest: NamedDomainObjectProvider<KotlinSourceSet>
    get() = this.named<KotlinSourceSet>("androidHostTest")
