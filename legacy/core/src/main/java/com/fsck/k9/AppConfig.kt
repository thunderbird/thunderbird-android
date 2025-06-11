package com.fsck.k9

interface AppConfig {
    val componentsToDisable: List<Class<*>>
}

class DefaultAppConfig(
    override val componentsToDisable: List<Class<*>>,
) : AppConfig
