package app.k9mail.feature.migration.qrcode.domain.usecase

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import app.k9mail.feature.migration.qrcode.domain.QrCodeDomainContract.UseCase
import app.k9mail.feature.migration.qrcode.domain.entity.AccountData.Account
import app.k9mail.feature.migration.qrcode.settings.XmlSettingWriter
import java.io.File

internal class QrCodeSettingsWriter(
    private val context: Context,
    private val xmlSettingWriter: XmlSettingWriter,
) : UseCase.QrCodeSettingsWriter {
    override fun write(accounts: List<Account>): Uri {
        val file = getSettingsFile()
        writeSettingsToFile(file, accounts)

        val authority = "${context.packageName}.qrcode.settings"
        return FileProvider.getUriForFile(context, authority, file)
    }

    private fun writeSettingsToFile(file: File, accounts: List<Account>) {
        file.outputStream().use { outputStream ->
            xmlSettingWriter.writeSettings(outputStream, accounts)
        }
    }

    private fun getSettingsFile(): File {
        return File(getDirectory(), FILENAME)
    }

    private fun getDirectory(): File {
        val directory = File(context.filesDir, DIRECTORY_NAME)
        directory.mkdirs()

        return directory
    }

    companion object {
        private const val DIRECTORY_NAME = "qrcode"
        private const val FILENAME = "settings.k9s"
    }
}
