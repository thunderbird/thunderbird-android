package net.thunderbird.core.preference.display.visualSettings.message.list

enum class MessageListDateTimeFormat {

    /**
     * The default setting with the following rules:
     *  - For messages received today: show only the time (localized)
     *      - Examples: 2:34PM or 14:34
     *  - For Yesterday: display "Yesterday" in localized text
     *  - Between 1 and 7 days ago, exclusive (not including yesterday or the current day): display Day of Week
     *      - Example: "Wednesday"
     *  - >= 7 days prior: display Month and Day for date: March 15th
     *  - >= 1 year prior: Full date with locale respected
     *      - Examples: "1/23/2026 1:23PM" "23/1/2026 13:23"
     */
    Contextual,

    /**
     * Full date and time for every item on the list
     *  - This matches the format of the 1 year prior in the contextual setting
     *      - Examples: "1/23/2026 1:23PM" "23/1/2026 13:23"
     */
    Full,

    /**
     * ISO 8601 option, which is often requested by power users who prefer a
     * deterministic, unambiguous format over the system locale defaults.
     *      - Examples: "2026-01-23 13:23"
     */
    ISO,
}
