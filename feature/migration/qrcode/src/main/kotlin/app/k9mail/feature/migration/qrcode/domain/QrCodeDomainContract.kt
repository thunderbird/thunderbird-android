package app.k9mail.feature.migration.qrcode.domain

import android.net.Uri
import app.k9mail.feature.migration.qrcode.domain.entity.AccountData
import app.k9mail.feature.migration.qrcode.domain.entity.AccountData.Account
import androidx.camera.core.UseCase as CameraUseCase

internal interface QrCodeDomainContract {

    interface UseCase {

        fun interface CameraUseCasesProvider {
            fun getUseCases(): List<CameraUseCase>
        }

        fun interface QrCodePayloadReader {
            fun read(payload: String): AccountData?
        }

        fun interface QrCodeSettingsWriter {
            fun write(accounts: List<Account>): Uri
        }
    }
}
