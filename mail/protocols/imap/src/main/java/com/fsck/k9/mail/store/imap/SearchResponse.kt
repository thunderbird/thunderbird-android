package com.fsck.k9.mail.store.imap

internal class SearchResponse private constructor(
    /**
     * @return A list of numbers from the SEARCH response(s).
     */
    val numbers: List<Long>,
) {
    companion object {
        @JvmStatic
        fun parse(responses: List<ImapResponse>): SearchResponse {
            val numbers = mutableListOf<Long>()

            for (response in responses) {
                parseSingleLine(response, numbers)
            }

            return SearchResponse(numbers)
        }

        private fun parseSingleLine(response: ImapResponse, numbers: MutableList<Long>) {
            if (response.isTagged ||
                response.size < 2 ||
                !ImapResponseParser.equalsIgnoreCase(
                    response[0],
                    Responses.SEARCH,
                )
            ) {
                return
            }

            val end = response.size
            for (i in 1..<end) {
                try {
                    val number = response.getLong(i)
                    numbers.add(number)
                } catch (_: NumberFormatException) {
                    return
                }
            }
        }
    }
}
