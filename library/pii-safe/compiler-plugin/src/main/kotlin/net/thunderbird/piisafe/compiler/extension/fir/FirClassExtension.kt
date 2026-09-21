package net.thunderbird.piisafe.compiler.extension.fir

import net.thunderbird.piisafe.annotation.PiiSafe
import net.thunderbird.piisafe.compiler.symbols.ProjectFqNames
import net.thunderbird.piisafe.compiler.symbols.asClassId
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.hasAnnotationSafe
import org.jetbrains.kotlin.fir.declarations.hasAnnotationWithClassId
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.type

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

/**
 * Checks whether this [FirClassSymbol] is annotated with the [PiiSafe.HasPii] annotation.
 *
 * @param session the FIR session used to resolve annotations
 * @return `true` if the class has the [PiiSafe.HasPii] annotation, `false` otherwise
 */
internal fun FirClassSymbol<*>.isAnnotatedWithHasPii(session: FirSession): Boolean {
    val classId = ProjectFqNames.PiiSafeHasPiiFqName.asClassId(ProjectFqNames.PiiSafeAnnotationsPackageFqName)
    return hasAnnotationWithClassId(classId = classId, session = session)
}

internal fun ConeKotlinType.containsHasPiiAnnotatedType(session: FirSession): Boolean {
    if (toClassSymbol(session)?.isAnnotatedWithHasPii(session) == true) return true

    return typeArguments.any { projection ->
        projection.type?.containsHasPiiAnnotatedType(session) == true
    }
}
