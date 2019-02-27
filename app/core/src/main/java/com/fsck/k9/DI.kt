package com.fsck.k9

import android.app.Application
import com.fsck.k9.core.BuildConfig
import org.koin.android.ext.koin.with
import org.koin.android.logger.AndroidLogger
import org.koin.core.Koin
import org.koin.core.KoinContext
import org.koin.core.parameter.ParameterDefinition
import org.koin.core.parameter.emptyParameterDefinition
import org.koin.core.scope.Scope
import org.koin.dsl.module.Module
import org.koin.log.EmptyLogger
import org.koin.standalone.StandAloneContext

object DI {
    @JvmStatic fun start(application: Application, modules: List<Module>) {
        @Suppress("ConstantConditionIf")
        Koin.logger = if (BuildConfig.DEBUG) AndroidLogger() else EmptyLogger()

        StandAloneContext.startKoin(modules) with application
    }

    @JvmOverloads
    @JvmStatic
    fun <T : Any> get(clazz: Class<T>,
                      scope: Scope? = null,
                      parameters: ParameterDefinition = emptyParameterDefinition()): T {
        val koinContext = StandAloneContext.getKoin().koinContext
        val kClass = clazz.kotlin

        return koinContext.get("", kClass, scope, parameters)
    }

}
