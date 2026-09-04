package net.thunderbird.piisafe.compiler.symbols

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

/**
 * Fully qualified names of the project declarations the compiler plugin resolves against.
 */
internal object ProjectFqNames {
    val PiiSafeAnnotationsPackageFqName = FqName("net.thunderbird.piisafe.annotation")
    val PiiSafeHasPiiFqName = FqName("net.thunderbird.piisafe.annotation.PiiSafe.HasPii")
    val PiiSafeMaskFqName = FqName("net.thunderbird.piisafe.annotation.PiiSafe.Mask")
    val PiiSafeHideFqName = FqName("net.thunderbird.piisafe.annotation.PiiSafe.Hide")
}

/**
 * Converts a [FqName] to a [ClassId] by treating the portion after the package name
 * as the class name.
 *
 * @param packageFqName the fully qualified package name to be used as the package
 *  part of the [ClassId]
 * @param isLocal whether the resulting [ClassId] denotes a local class
 * @return a [ClassId] with the given package and the remaining relative class name
 */
internal fun FqName.asClassId(packageFqName: FqName, isLocal: Boolean = false): ClassId {
    val relativeClassName = FqName(asString().removePrefix("${packageFqName.asString()}."))
    return ClassId(packageFqName, relativeClassName, isLocal = isLocal)
}
