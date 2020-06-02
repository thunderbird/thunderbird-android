package com.fsck.k9.backend.jmap

import java.io.InputStream
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.buffer
import okio.source

fun createMockWebServer(vararg mockResponses: MockResponse): MockWebServer {
    return MockWebServer().apply {
        for (mockResponse in mockResponses) {
            enqueue(mockResponse)
        }
        start()
    }
}

fun responseBodyFromResource(name: String): MockResponse {
    return MockResponse().setBody(loadResource(name))
}

fun MockWebServer.skipRequests(count: Int) {
    repeat(count) {
        takeRequest()
    }
}

fun loadResource(name: String): String {
    val resourceAsStream = ResourceLoader.getResourceAsStream(name) ?: error("Couldn't load resource: $name")
    return resourceAsStream.use { it.source().buffer().readUtf8() }
}

private object ResourceLoader {
    fun getResourceAsStream(name: String): InputStream? = javaClass.getResourceAsStream(name)
}
