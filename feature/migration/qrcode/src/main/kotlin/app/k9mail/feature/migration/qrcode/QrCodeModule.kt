package app.k9mail.feature.migration.qrcode

import app.k9mail.feature.migration.qrcode.domain.QrCodeDomainContract.UseCase
import app.k9mail.feature.migration.qrcode.domain.usecase.QrCodePayloadReader
import app.k9mail.feature.migration.qrcode.domain.usecase.QrCodeSettingsWriter
import app.k9mail.feature.migration.qrcode.payload.QrCodePayloadAdapter
import app.k9mail.feature.migration.qrcode.payload.QrCodePayloadMapper
import app.k9mail.feature.migration.qrcode.payload.QrCodePayloadParser
import app.k9mail.feature.migration.qrcode.payload.QrCodePayloadValidator
import app.k9mail.feature.migration.qrcode.settings.DefaultUuidGenerator
import app.k9mail.feature.migration.qrcode.settings.UuidGenerator
import app.k9mail.feature.migration.qrcode.settings.XmlSettingWriter
import app.k9mail.feature.migration.qrcode.ui.QrCodeScannerViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val qrCodeModule = module {
    viewModel {
        QrCodeScannerViewModel(
            qrCodePayloadReader = get(),
            qrCodeSettingsWriter = get(),
        )
    }

    factory { QrCodePayloadAdapter() }
    factory { QrCodePayloadParser(qrCodePayloadAdapter = get()) }
    factory { QrCodePayloadValidator() }
    factory {
        QrCodePayloadMapper(
            qrCodePayloadValidator = get(),
            deletePolicyProvider = get(),
        )
    }

    factory<UseCase.QrCodePayloadReader> {
        QrCodePayloadReader(
            parser = get(),
            mapper = get(),
        )
    }

    factory<UseCase.QrCodeSettingsWriter> {
        QrCodeSettingsWriter(
            context = get(),
            xmlSettingWriter = get(),
        )
    }

    factory<UuidGenerator> { DefaultUuidGenerator() }
    factory {
        XmlSettingWriter(
            uuidGenerator = get(),
        )
    }
}
