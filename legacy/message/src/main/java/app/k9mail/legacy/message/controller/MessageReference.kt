package app.k9mail.legacy.message.controller

import com.fsck.k9.mail.filter.Base64
import java.util.StringTokenizer

data class MessageReference(
    val accountUuid: String,
    val folderId: Long,
    val uid: String,
) {
    fun toIdentityString(): String {
        return buildString {
            append(IDENTITY_VERSION_2)
            append(IDENTITY_SEPARATOR)
            append(Base64.encode(accountUuid))
            append(IDENTITY_SEPARATOR)
            append(Base64.encode(folderId.toString()))
            append(IDENTITY_SEPARATOR)
            append(Base64.encode(uid))
        }
    }

    fun equals(accountUuid: String, folderId: Long, uid: String): Boolean {
        return this.accountUuid == accountUuid && this.folderId == folderId && this.uid == uid
    }

    fun withModifiedUid(newUid: String): MessageReference {
        return copy(uid = newUid)
    }

    companion object {
        private const val IDENTITY_VERSION_2 = '#'
        private const val IDENTITY_SEPARATOR = ":"

        @Suppress("ReturnCount", "MagicNumber")
        @JvmStatic
        fun parse(identity: String?): MessageReference? {
            if (identity == null || identity.isEmpty() || identity[0] != IDENTITY_VERSION_2) {
                return null
            }

            val tokens = StringTokenizer(identity.substring(2), IDENTITY_SEPARATOR, false)
            if (tokens.countTokens() < 3) {
                return null
            }

            val accountUuid = Base64.decode(tokens.nextToken())
            val folderId = Base64.decode(tokens.nextToken()).toLong()
            val uid = Base64.decode(tokens.nextToken())
            return MessageReference(accountUuid, folderId, uid)
        }
    }
}
