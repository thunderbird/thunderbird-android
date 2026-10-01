package net.thunderbird.core.featureflag.data

import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.cio.CIO

actual fun createHttpClientEngine(): HttpClientEngineFactory<HttpClientEngineConfig> = CIO
