package net.thunderbird.quality.ui

/**
 * Existing violations of the MVI rules in [ValidateMviConventions], as `<path>::<name>`.
 *
 * Only remove entries: fix the code, then delete its line. The rules fail on new violations and on entries that no
 * longer match anything.
 */
internal object MviBaseline {
    val viewModelsWithoutBase = setOf(
        "components/ui/catalog/src/main/kotlin/net/thunderbird/components/ui/catalog/ui/CatalogViewModel.kt::CatalogViewModel",
        "components/ui/catalog/src/main/kotlin/net/thunderbird/components/ui/catalog/ui/page/CatalogPageViewModel.kt::CatalogPageViewModel",
        "feature/changelog/internal/src/main/kotlin/net/thunderbird/feature/changelog/internal/ui/RecentChangesViewModel.kt::RecentChangesViewModel",
        "feature/settings/import/src/main/kotlin/app/k9mail/feature/settings/import/ui/AuthViewModel.kt::AuthViewModel",
        "feature/settings/import/src/main/kotlin/app/k9mail/feature/settings/import/ui/PickAppViewModel.kt::PickAppViewModel",
        "feature/settings/import/src/main/kotlin/app/k9mail/feature/settings/import/ui/SettingsImportViewModel.kt::SettingsImportViewModel",
        "legacy/ui/legacy/src/main/java/com/fsck/k9/ui/choosefolder/ChooseFolderViewModel.kt::ChooseFolderViewModel",
        "legacy/ui/legacy/src/main/java/com/fsck/k9/ui/endtoend/AutocryptKeyTransferViewModel.kt::AutocryptKeyTransferViewModel",
        "legacy/ui/legacy/src/main/java/com/fsck/k9/ui/managefolders/FolderSettingsViewModel.kt::FolderSettingsViewModel",
        "legacy/ui/legacy/src/main/java/com/fsck/k9/ui/managefolders/ManageFoldersViewModel.kt::ManageFoldersViewModel",
        "legacy/ui/legacy/src/main/java/com/fsck/k9/ui/messagedetails/MessageDetailsViewModel.kt::MessageDetailsViewModel",
        "legacy/ui/legacy/src/main/java/com/fsck/k9/ui/messagelist/MessageListViewModel.kt::MessageListViewModel",
        "legacy/ui/legacy/src/main/java/com/fsck/k9/ui/messagesource/MessageHeadersViewModel.kt::MessageHeadersViewModel",
        "legacy/ui/legacy/src/main/java/com/fsck/k9/ui/settings/SettingsViewModel.kt::SettingsViewModel",
        "legacy/ui/legacy/src/main/java/com/fsck/k9/ui/settings/account/AccountSettingsViewModel.kt::AccountSettingsViewModel",
        "legacy/ui/legacy/src/main/java/com/fsck/k9/ui/settings/general/GeneralSettingsViewModel.kt::GeneralSettingsViewModel",
    )

    val incompleteUiContracts = setOf(
        "components/ui/catalog/src/main/kotlin/net/thunderbird/components/ui/catalog/ui/CatalogContract.kt::CatalogContract",
        "components/ui/catalog/src/main/kotlin/net/thunderbird/components/ui/catalog/ui/page/CatalogPageContract.kt::CatalogPageContract",
    )

    val viewModelsOutsideUiPackage = setOf(
        "legacy/ui/legacy/src/main/java/com/fsck/k9/activity/setup/AccountSetupCompositionViewModel.kt::AccountSetupCompositionViewModel",
    )

    val screensOutsideUiPackage = setOf(
        "feature/onboarding/migration/api/src/main/kotlin/app/k9mail/feature/onboarding/migration/api/OnboardingMigrationManager.kt::OnboardingMigrationScreen",
        "feature/onboarding/migration/noop/src/main/kotlin/app/k9mail/feature/onboarding/migration/noop/NoOpOnboardingMigrationManager.kt::OnboardingMigrationScreen",
        "feature/onboarding/migration/thunderbird/src/main/kotlin/app/k9mail/feature/onboarding/migration/thunderbird/TbOnboardingMigrationManager.kt::OnboardingMigrationScreen",
        "feature/onboarding/migration/thunderbird/src/main/kotlin/app/k9mail/feature/onboarding/migration/thunderbird/TbOnboardingMigrationScreen.kt::TbOnboardingMigrationScreen",
        "legacy/ui/legacy/src/main/java/com/fsck/k9/activity/setup/AccountSetupComposition.kt::AccountSetupCompositionScreen",
    )

    val composablesTakingViewModel = setOf(
        "feature/account/oauth/src/main/kotlin/app/k9mail/feature/account/oauth/ui/AccountOAuthView.kt::AccountOAuthView",
        "feature/account/server/validation/src/main/kotlin/app/k9mail/feature/account/server/validation/ui/ServerValidationContent.kt::ServerValidationContent",
        "feature/account/setup/src/main/kotlin/app/k9mail/feature/account/setup/ui/autodiscovery/AccountAutoDiscoveryContent.kt::AccountAutoDiscoveryContent",
        "feature/account/setup/src/main/kotlin/app/k9mail/feature/account/setup/ui/autodiscovery/AccountAutoDiscoveryContent.kt::AutoDiscoveryContent",
        "feature/account/setup/src/main/kotlin/app/k9mail/feature/account/setup/ui/autodiscovery/AccountAutoDiscoveryContent.kt::ContentView",
    )
}
