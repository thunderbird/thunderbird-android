package app.k9mail.legacy.account

@Suppress("MagicNumber")
enum class DeletePolicy(@JvmField val setting: Int) {
    NEVER(0),
    SEVEN_DAYS(1),
    ON_DELETE(2),
    MARK_AS_READ(3),
    ;

    companion object {
        fun fromInt(initialSetting: Int): DeletePolicy {
            return entries.find { it.setting == initialSetting } ?: error("DeletePolicy $initialSetting unknown")
        }
    }
}
