package com.fsck.k9.testing

import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.kotlin.KStubbing

object MockHelper {
    @JvmStatic
    fun <T> mockBuilder(classToMock: Class<T>): T {
        return mock(classToMock) { invocation ->
            val mock = invocation.mock
            if (invocation.method.returnType.isInstance(mock)) {
                mock
            } else {
                Mockito.RETURNS_DEFAULTS.answer(invocation)
            }
        }
    }

    inline fun <reified T : Any> mockBuilder(stubbing: KStubbing<T>.(T) -> Unit = {}): T {
        return mockBuilder(T::class.java).apply { KStubbing(this).stubbing(this) }
    }
}
