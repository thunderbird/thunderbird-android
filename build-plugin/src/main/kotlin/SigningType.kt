enum class SigningType(
    val app: String,
    val type: String,
    val id: String = "$app.$type",
) {
    K9_RELEASE(app = "k9", type = "release"),
}
