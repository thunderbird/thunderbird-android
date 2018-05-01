package com.fsck.k9.mail.internet

object Headers {
    @JvmStatic
    fun contentType(mimeType: String, name: String): String {
        return MimeParameterEncoder.encode(mimeType, mapOf("name" to name))
    }

    @JvmStatic
    @JvmOverloads
    fun contentDisposition(disposition: String, fileName: String, size: Long? = null): String {
        val parameters = if (size == null) {
            mapOf("filename" to fileName)
        } else {
            mapOf("filename" to fileName, "size" to size.toString())
        }

        return MimeParameterEncoder.encode(disposition, parameters)
    }
}
