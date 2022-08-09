package com.fsck.k9.backend.jmap

import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutionException
import rs.ltt.jmap.client.JmapRequest
import rs.ltt.jmap.client.MethodResponses
import rs.ltt.jmap.client.session.Session
import rs.ltt.jmap.common.entity.capability.CoreCapability
import rs.ltt.jmap.common.method.MethodResponse

internal const val MAX_CHUNK_SIZE = 5000

internal inline fun <reified T : MethodResponse> ListenableFuture<MethodResponses>.getMainResponseBlocking(): T {
    return futureGetOrThrow().getMain(T::class.java)
}

internal inline fun <reified T : MethodResponse> JmapRequest.Call.getMainResponseBlocking(): T {
    return methodResponses.getMainResponseBlocking()
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <T> ListenableFuture<T>.futureGetOrThrow(): T {
    return try {
        get()
    } catch (e: ExecutionException) {
        throw e.cause ?: e
    }
}

internal val Session.maxObjectsInGet: Int
    get() {
        val coreCapability = getCapability(CoreCapability::class.java)
        return coreCapability.maxObjectsInGet.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

internal val Session.maxObjectsInSet: Int
    get() {
        val coreCapability = getCapability(CoreCapability::class.java)
        return coreCapability.maxObjectsInSet.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }
