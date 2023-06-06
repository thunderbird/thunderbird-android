package app.k9mail.autodiscovery.autoconfig

import java.io.InputStream

/**
 * Result type for [HttpFetcher].
 */
internal sealed interface HttpFetchResult {
    /**
     * The HTTP request returned a success response.
     *
     * @param inputStream Contains the response body.
     */
    data class SuccessResponse(
        val inputStream: InputStream,
    ) : HttpFetchResult

    /**
     * The server returned an error response.
     *
     * @param code The HTTP status code.
     */
    data class ErrorResponse(val code: Int) : HttpFetchResult
}
