package com.fsck.k9.backend.api

import java.io.InterruptedIOException
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/** Retry policy for safe REST reads. Never replay requests that can mutate server state. */
object TransientApiRequestRetry {
    const val MAX_ATTEMPTS = 3
    const val ORIGINAL_RETRY_AFTER_HEADER = "X-Thunderbird-Original-Retry-After"
    private const val MAX_RETRY_AFTER_MILLIS = 30_000L
    private const val INITIAL_BACKOFF_MILLIS = 250L
    private const val MILLISECONDS_PER_SECOND = 1_000L

    private const val HTTP_REQUEST_TIMEOUT = 408
    private const val HTTP_TOO_MANY_REQUESTS = 429
    private const val HTTP_INTERNAL_SERVER_ERROR = 500
    private const val HTTP_BAD_GATEWAY = 502
    private const val HTTP_SERVICE_UNAVAILABLE = 503
    private const val HTTP_GATEWAY_TIMEOUT = 504

    private val RETRYABLE_STATUS_CODES = setOf(
        HTTP_REQUEST_TIMEOUT,
        HTTP_TOO_MANY_REQUESTS,
        HTTP_INTERNAL_SERVER_ERROR,
        HTTP_BAD_GATEWAY,
        HTTP_SERVICE_UNAVAILABLE,
        HTTP_GATEWAY_TIMEOUT,
    )

    fun shouldRetryTransport(method: String, attempt: Int): Boolean =
        isSafeRead(method) && attempt < MAX_ATTEMPTS

    fun shouldRetryResponse(method: String, statusCode: Int, attempt: Int): Boolean =
        isSafeRead(method) && attempt < MAX_ATTEMPTS && statusCode in RETRYABLE_STATUS_CODES

    fun waitBeforeRetry(attempt: Int, retryAfterHeader: String? = null) {
        val delayMillis = parseRetryAfter(retryAfterHeader)
            ?.coerceAtMost(MAX_RETRY_AFTER_MILLIS)
            ?: (INITIAL_BACKOFF_MILLIS shl (attempt - 1).coerceIn(0, 4))
        if (delayMillis == 0L) return

        try {
            Thread.sleep(delayMillis)
        } catch (error: InterruptedException) {
            Thread.currentThread().interrupt()
            throw InterruptedIOException("Interrupted while waiting to retry an API request").apply {
                initCause(error)
            }
        }
    }

    private fun isSafeRead(method: String): Boolean = method.equals("GET", ignoreCase = true)

    private fun parseRetryAfter(value: String?): Long? {
        val header = value?.trim()?.takeIf(String::isNotEmpty)
        return header?.let { retryAfter ->
            val seconds = retryAfter.toLongOrNull()
            when {
                seconds == null -> runCatching {
                    val retryAt = ZonedDateTime.parse(retryAfter, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()
                    Duration.between(Instant.now(), retryAt).toMillis().coerceAtLeast(0L)
                }.getOrNull()

                seconds < 0 -> null
                else -> runCatching { Math.multiplyExact(seconds, MILLISECONDS_PER_SECOND) }.getOrNull()
            }
        }
    }
}
