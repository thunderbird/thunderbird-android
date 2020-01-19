package com.fsck.k9.backend.jmap

import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutionException
import rs.ltt.jmap.client.JmapRequest
import rs.ltt.jmap.client.MethodResponses
import rs.ltt.jmap.common.method.MethodResponse

internal inline fun <reified T : MethodResponse> ListenableFuture<MethodResponses>.getMainResponseBlocking(): T {
    return futureGetOrThrow().getMain(T::class.java)
}

internal inline fun <reified T : MethodResponse> JmapRequest.Call.getMainResponseBlocking(): T {
    return methodResponses.getMainResponseBlocking()
}

internal inline fun <T> ListenableFuture<T>.futureGetOrThrow(): T {
    return try {
        get()
    } catch (e: ExecutionException) {
        throw e.cause ?: e
    }
}
