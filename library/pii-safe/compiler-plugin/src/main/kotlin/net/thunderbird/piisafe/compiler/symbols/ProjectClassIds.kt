package net.thunderbird.piisafe.compiler.symbols

internal object ProjectClassIds {
    val PiiSafeMaskClassId = ProjectFqNames.PiiSafeMaskFqName.asClassId(ProjectFqNames.PiiSafeAnnotationsPackageFqName)
    val PiiSafeHideClassId = ProjectFqNames.PiiSafeHideFqName.asClassId(ProjectFqNames.PiiSafeAnnotationsPackageFqName)
}
