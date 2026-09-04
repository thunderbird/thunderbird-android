package net.thunderbird.piisafe.compiler.extension.fir

import net.thunderbird.piisafe.annotation.PiiSafe
import net.thunderbird.piisafe.compiler.symbols.ProjectFqNames
import net.thunderbird.piisafe.compiler.symbols.asClassId
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.hasAnnotationSafe

/**
 * Checks if this [FirClass] is annotated with the [PiiSafe.HasPii] annotation.
 *
 * @param session the FIR session used to resolve and check annotations
 * @return `true` if the class has the [PiiSafe.HasPii] annotation, `false` otherwise
 */
internal fun FirClass.isAnnotatedWithHasPii(session: FirSession): Boolean {
    val classId = ProjectFqNames.PiiSafeHasPiiFqName.asClassId(ProjectFqNames.PiiSafeAnnotationsPackageFqName)
    return hasAnnotationSafe(classId = classId, session = session)
}
