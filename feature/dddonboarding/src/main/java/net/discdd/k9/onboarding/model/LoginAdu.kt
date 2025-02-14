package net.discdd.k9.onboarding.model

data class LoginAdu(
    val email: String,
    override val password: String
): Adu {
    override fun toByteArray(): ByteArray {
        return "login\n$email\n$password".toByteArray()
    }
}

data class AcknowledgementLoginAdu(
    override val email: String?,
    override val password: String?,
    override val success: Boolean,
    override val message: String?
): AcknowledgementAdu {
    companion object {
        fun toAckLoginAdu(data: String): AcknowledgementLoginAdu? {
            val dataArr = data.split("\n")
            if (dataArr.size < 4 || dataArr[0] != "login-ack") return null;

            if (dataArr[1] == "success") {
                return AcknowledgementLoginAdu(
                    email = dataArr[3],
                    password = dataArr[4],
                    success = true,
                    message = null
                )
            }
            return AcknowledgementLoginAdu(
                email = null,
                password = null,
                success = false,
                message = dataArr[2]
            )
        }
    }
}
