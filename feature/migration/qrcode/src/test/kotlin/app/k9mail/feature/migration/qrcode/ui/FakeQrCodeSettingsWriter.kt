package app.k9mail.feature.migration.qrcode.ui

import android.net.Uri
import androidx.core.net.toUri
import app.k9mail.feature.migration.qrcode.domain.QrCodeDomainContract.UseCase
import app.k9mail.feature.migration.qrcode.domain.entity.AccountData.Account

internal class FakeQrCodeSettingsWriter : UseCase.QrCodeSettingsWriter {
    var arguments: List<Account>? = null

    override fun write(accounts: List<Account>): Uri {
        check(arguments == null) { "write() called more than once" }

        arguments = accounts
        return "content://provider".toUri()
    }
}
