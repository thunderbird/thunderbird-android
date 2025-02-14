package net.discdd.k9.onboarding.model

data class RegisterAdu(
    val prefix1: String,
    val prefix2: String,
    val prefix3: String,
    val suffix1: String,
    val suffix2: String,
    val suffix3: String,
    override val password: String
): Adu {
    override fun toByteArray(): ByteArray {
        return "register\n$prefix1,$prefix2,$prefix3\n$suffix1,$suffix2,$suffix3\n$password".toByteArray()
    }
}

data class AcknowledgementRegisterAdu(
    override val email: String?,
    override val password: String?,
    override val success: Boolean,
    override val message: String?
): AcknowledgementAdu {
    companion object {
        fun toAckRegisterAdu(data: String): AcknowledgementRegisterAdu? {
            val dataArr = data.split("\n")
            if (dataArr.size < 4 || dataArr[0] != "register-ack") return null

            if (dataArr[1] == "success") {
                return AcknowledgementRegisterAdu(email = dataArr[3], password = dataArr[4], success = true, message = null)
            }

            return AcknowledgementRegisterAdu(email = null, password = null, success = false, message = dataArr[2])
        }
    }
}
