package app.k9mail.feature.account.common.domain.entity

data class SpecialFolderOptions(
    val archiveSpecialFolderOptions: List<SpecialFolderOption>,
    val draftsSpecialFolderOptions: List<SpecialFolderOption>,
    val sentSpecialFolderOptions: List<SpecialFolderOption>,
    val spamSpecialFolderOptions: List<SpecialFolderOption>,
    val trashSpecialFolderOptions: List<SpecialFolderOption>,
)
