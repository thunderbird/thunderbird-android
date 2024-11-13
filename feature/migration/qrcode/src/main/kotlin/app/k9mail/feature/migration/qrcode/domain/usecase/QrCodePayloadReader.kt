package app.k9mail.feature.migration.qrcode.domain.usecase

import app.k9mail.feature.migration.qrcode.domain.QrCodeDomainContract.UseCase
import app.k9mail.feature.migration.qrcode.domain.entity.AccountData
import app.k9mail.feature.migration.qrcode.payload.QrCodePayloadMapper
import app.k9mail.feature.migration.qrcode.payload.QrCodePayloadParser

internal class QrCodePayloadReader(
    private val parser: QrCodePayloadParser,
    private val mapper: QrCodePayloadMapper,
) : UseCase.QrCodePayloadReader {
    override fun read(payload: String): AccountData? {
        val parsedData = parser.parse(payload) ?: return null

        return mapper.toAccountData(parsedData)
    }
}
