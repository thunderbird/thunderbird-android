package net.thunderbird.piisafe.compiler

import org.jetbrains.kotlin.GeneratedDeclarationKey

/**
 * Marks FIR/IR declarations synthesized by this plugin (e.g. `toStringPiiSafe()`), so the IR
 * generation phase can identify the declarations the FIR phase created.
 */
internal object PiiSafePluginKey : GeneratedDeclarationKey()
