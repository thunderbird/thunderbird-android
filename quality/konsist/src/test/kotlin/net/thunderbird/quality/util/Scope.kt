package net.thunderbird.quality.util

import com.lemonappdev.konsist.api.Konsist

val projectScope = Konsist.scopeFromProject()
val productionScope = Konsist.scopeFromProduction()
val testScope = Konsist.scopeFromTest()
