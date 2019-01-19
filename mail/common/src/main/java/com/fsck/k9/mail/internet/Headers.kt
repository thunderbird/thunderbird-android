package com.fsck.k9.mail.internet

object Headers {
    @JvmStatic
    fun contentType(mimeType: String, name: String): String {
        return MimeParameterEncoder.encode(mimeType, mapOf("name" to name))
    }

    @JvmStatic
    fun contentType(mimeType: String, charset: String, name: String?): String {
        val parameters = if (name == null) {
            mapOf("charset" to charset)
        } else {
            mapOf("charset" to charset, "name" to name)
        }

        return MimeParameterEncoder.encode(mimeType, parameters)
    }

    @JvmStatic
    fun contentTypeForMultipart(mimeType: String, boundary: String): String {
        return MimeParameterEncoder.encode(mimeType, mapOf("boundary" to boundary))
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
