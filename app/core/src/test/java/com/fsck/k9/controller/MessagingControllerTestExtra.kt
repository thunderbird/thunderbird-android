@file:JvmName("MessagingControllerTestExtra")
package com.fsck.k9.controller

import com.fsck.k9.backend.BackendManager
import com.fsck.k9.backend.api.Backend
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import org.koin.dsl.module.applicationContext
import org.koin.standalone.StandAloneContext.loadKoinModules

fun backendManagerProvides(backend: Backend) {
    val backendManager = mock<BackendManager> {
        on { getBackend(any()) } doReturn backend
    }

    loadKoinModules(applicationContext {
        bean { backendManager }
    })
}
