# Java to Kotlin Conversion Guide

This guide describes our process for converting Java code to Kotlin.

## Why Convert to Kotlin?

Java and Kotlin are compatible languages, but we decided to convert our codebase to Kotlin for the following reasons:

- Kotlin is more concise and expressive than Java.
- Kotlin has better support for null safety.
- Kotlin has a number of modern language features that make it easier to write maintainable code.

See our [ADR-0001](../architecture/adr/0001-switch-from-java-to-kotlin.md) for more information.

## How to Convert Java Code to Kotlin

1. Write tests for any code that is not adequately covered by tests.
2. Use the "Convert Java File to Kotlin File" action in IntelliJ or Android Studio to convert the Java code.
3. Fix any issues that prevent the code from compiling after the automatic conversion.
4. Commit the changes as separate commits:
   1. The change of file extension (e.g. `example.java` -> `example.kt`).
   2. The conversion of the Java file to Kotlin.
   - This can be automated by IntelliJ/Android Studio if you use their VCS integration and enable the option to commit changes separately.
5. Refactor the code to improve readability and maintainability. This includes:
   1. Removing unnecessary code.
   2. Using Kotlin's standard library functions, language features, null safety and coding conventions.

## Additional Tips

- Use `when` expressions instead of `if-else` statements.
- Use `apply` and `also` to perform side effects on objects.
- Use `@JvmField` to expose a Kotlin property as a field in Java.

## Resources

- [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- [Calling Kotlin from Java](https://kotlinlang.org/docs/java-to-kotlin-interop.html)
- [Calling Java from Kotlin](https://kotlinlang.org/docs/java-interop.html)
- [Kotlin and Android | Android Developers](https://developer.android.com/kotlin?hl=en)
