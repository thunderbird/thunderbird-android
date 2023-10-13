package app.k9mail.feature.account.common.domain.entity

data class SpecialFolderSettings(
    val archiveSpecialFolderOption: SpecialFolderOption,
    val draftsSpecialFolderOption: SpecialFolderOption,
    val sentSpecialFolderOption: SpecialFolderOption,
    val spamSpecialFolderOption: SpecialFolderOption,
    val trashSpecialFolderOption: SpecialFolderOption,
)
