package app.k9mail.feature.migration.qrcode

import app.k9mail.feature.migration.qrcode.ui.QrCodeScannerViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val qrCodeModule = module {
    viewModel { QrCodeScannerViewModel() }
}
