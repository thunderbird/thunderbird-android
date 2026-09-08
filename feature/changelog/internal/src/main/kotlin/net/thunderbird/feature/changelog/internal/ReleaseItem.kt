package net.thunderbird.feature.changelog.internal

data class ReleaseItem(
    val versionCode: Int,
    val versionName: String,
    val date: String?,
    val changes: List<String>,
) {
    override fun toString(): String {
        return "ReleaseItem{" +
            "versionCode=" + versionCode +
            ", versionName='" + versionName + '\'' +
            ", date='" + date + '\'' +
            ", changes=" + changes +
            '}'
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o == null || javaClass != o.javaClass) {
            return false
        }

        val that = o as ReleaseItem

        if (versionCode != that.versionCode) {
            return false
        }
        if (versionName != that.versionName) {
            return false
        }
        if (if (date != null) (date != that.date) else that.date != null) {
            return false
        }
        return changes == that.changes
    }
    override fun hashCode(): Int {
        var result = versionCode
        result = 31 * result + versionName.hashCode()
        result = 31 * result + (date?.hashCode() ?: 0)
        result = 31 * result + changes.hashCode()
        return result
    }
}
