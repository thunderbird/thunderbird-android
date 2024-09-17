package app.k9mail.feature.migration.qrcode

internal class QrCodePayloadReader(
    private val parser: QrCodePayloadParser = QrCodePayloadParser(),
    private val mapper: QrCodePayloadMapper = QrCodePayloadMapper(),
) {
    fun read(payload: String): AccountData? {
        val parsedData = parser.parse(payload) ?: return null

        return mapper.toAccountData(parsedData)
    }
}
