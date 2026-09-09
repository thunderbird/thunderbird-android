package net.thunderbird.piisafe.compiler.testing

/**
 * Reads a `.kt` fixture file used as compilation input by a `kotlin-compile-testing` test, so large
 * source snippets can live as plain, syntax-highlighted Kotlin files instead of inline string literals
 * in the test class.
 *
 * Resolved relative to the receiver's package, under a `fixtures/` resource directory, e.g. a test in
 * `net.thunderbird.piisafe.compiler.ir` reads
 * `src/test/resources/fixtures/<name>`.
 */
internal fun Any.fixture(name: String): String {
    val resourcePath = "fixtures/$name"
    val stream = checkNotNull(this::class.java.classLoader.getResourceAsStream(resourcePath)) {
        "Fixture not found: $resourcePath (relative to ${this::class.java.packageName})"
    }
    return stream.bufferedReader().use { it.readText() }
}
