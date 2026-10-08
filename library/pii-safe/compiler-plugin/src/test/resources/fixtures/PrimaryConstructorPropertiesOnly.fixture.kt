import net.thunderbird.piisafe.annotation.PiiSafe

@PiiSafe.HasPii
data class Session(
    @get:PiiSafe.Mask val email: String,
) {
    private val secretId = 1234
    private val authToken get() = "TOP-SECRET"
}
