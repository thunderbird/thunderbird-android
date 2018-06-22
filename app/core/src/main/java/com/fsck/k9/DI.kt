package com.fsck.k9

import android.app.Application
import com.fsck.k9.core.BuildConfig
import org.koin.Koin
import org.koin.KoinContext
import org.koin.android.ext.koin.with
import org.koin.android.logger.AndroidLogger
import org.koin.core.parameter.Parameters
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
    fun <T : Any> get(clazz: Class<T>, name: String = "", parameters: Parameters = { emptyMap() }): T {
        val koinContext = StandAloneContext.koinContext as KoinContext
        val kClass = clazz.kotlin

        return if (name.isEmpty()) {
            koinContext.resolveInstance(kClass, parameters) { koinContext.beanRegistry.searchAll(kClass) }
        } else {
            koinContext.resolveInstance(kClass, parameters) { koinContext.beanRegistry.searchByName(name) }
        }
    }

    inline fun <reified T : Any> get(name: String = "", noinline parameters: Parameters = { emptyMap() }): T {
        val koinContext = StandAloneContext.koinContext as KoinContext
        return koinContext.get(name, parameters)
    }
}
