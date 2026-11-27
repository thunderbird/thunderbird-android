package net.thunderbird.quality.testing

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import java.io.File
import kotlin.test.Test
import net.thunderbird.quality.util.QualityViolation
import net.thunderbird.quality.util.assertNoQualityViolations
import net.thunderbird.quality.util.projectScope

class ValidateMockitoUsage {

    @Test
    fun `Mockito should only be used in DI tests or existing exceptions`() {
        assertNoQualityViolations(
            rule = "Mockito usage outside DI tests",
            violations = mockitoUsageViolations(projectScope.files, Konsist.projectRootPath, existingMockitoUsers),
        )
    }

    private companion object {
        // Existing usage only. Remove entries when Mockito is removed; do not add new non-DI usage.
        val existingMockitoUsers = """
            app-common/src/test/kotlin/net/thunderbird/app/common/account/AccountCreatorTest.kt
            app-common/src/test/kotlin/net/thunderbird/app/common/account/DefaultAccountDefaultsProviderTest.kt
            app-common/src/test/kotlin/net/thunderbird/app/common/feature/mail/message/data/repository/DefaultMessageQueryRepositoryTest.kt
            backend/imap/src/test/java/com/fsck/k9/backend/imap/ImapSyncTest.kt
            core/android/common/src/test/kotlin/net/thunderbird/core/android/common/camera/CameraCaptureHandlerTest.kt
            core/android/common/src/test/kotlin/net/thunderbird/core/android/common/contact/CachingContactRepositoryTest.kt
            core/android/common/src/test/kotlin/net/thunderbird/core/android/common/contact/ContentResolverContactDataSourceTest.kt
            core/android/testing/src/main/kotlin/net/thunderbird/core/android/testing/MockHelper.kt
            core/logging/internal/src/androidHostTest/kotlin/net/thunderbird/core/logging/internal/LogcatExporterTest.kt
            feature/account/oauth/src/test/kotlin/app/k9mail/feature/account/oauth/data/AuthorizationRepositoryTest.kt
            feature/account/oauth/src/test/kotlin/app/k9mail/feature/account/oauth/domain/usecase/FinishOAuthSignInTest.kt
            feature/changelog/internal/src/test/kotlin/net/thunderbird/feature/changelog/internal/ChangelogProviderTest.kt
            feature/changelog/internal/src/test/kotlin/net/thunderbird/feature/changelog/internal/ChangelogVersionHistoryTest.kt
            feature/funding/googleplay/src/test/kotlin/net/thunderbird/feature/funding/googleplay/data/mapper/ProductDetailsMapperTest.kt
            feature/funding/googleplay/src/test/kotlin/net/thunderbird/feature/funding/googleplay/data/remote/bilingclient/BillingClientTest.kt
            feature/funding/googleplay/src/test/kotlin/net/thunderbird/feature/funding/googleplay/data/remote/bilingclient/BillingConnectorTest.kt
            feature/funding/googleplay/src/test/kotlin/net/thunderbird/feature/funding/googleplay/ui/reminder/FragmentLifecycleObserverTest.kt
            feature/funding/googleplay/src/test/kotlin/net/thunderbird/feature/funding/googleplay/ui/reminder/FundingReminderTest.kt
            feature/navigation/drawer/dropdown/src/test/kotlin/net/thunderbird/feature/navigation/drawer/dropdown/domain/usecase/GetDrawerConfigTest.kt
            feature/notification/impl/src/androidHostTest/kotlin/net/thunderbird/feature/notification/impl/intent/action/DefaultNotificationActionIntentCreatorTest.kt
            feature/settings/import/src/test/kotlin/app/k9mail/feature/settings/import/ui/SettingsImportViewModelTest.kt
            feature/widget/unread/src/test/kotlin/app/k9mail/feature/widget/unread/UnreadWidgetDataProviderTest.kt
            feature/widget/unread/src/test/kotlin/app/k9mail/feature/widget/unread/UnreadWidgetMigrationsTest.kt
            legacy/core/src/test/java/com/fsck/k9/controller/DefaultMessageCountsProviderTest.kt
            legacy/core/src/test/java/com/fsck/k9/controller/MessagingControllerRefreshFolderListTest.kt
            legacy/core/src/test/java/com/fsck/k9/controller/MessagingControllerRemovedAccountTest.kt
            legacy/core/src/test/java/com/fsck/k9/controller/NotificationOperationsTest.kt
            legacy/core/src/test/java/com/fsck/k9/helper/MessageHelperTest.kt
            legacy/core/src/test/java/com/fsck/k9/mailstore/folder/DefaultOutboxFolderManagerTest.kt
            legacy/core/src/test/java/com/fsck/k9/mailstore/K9BackendDefaultStorageTest.kt
            legacy/core/src/test/java/com/fsck/k9/mailstore/K9BackendFolderTest.kt
            legacy/core/src/test/java/com/fsck/k9/mailstore/MessageListCacheTest.kt
            legacy/core/src/test/java/com/fsck/k9/mailstore/MessageListRepositoryTest.kt
            legacy/core/src/test/java/com/fsck/k9/message/html/HtmlConverterTest.kt
            legacy/core/src/test/java/com/fsck/k9/message/quote/QuoteDateFormatterTest.kt
            legacy/core/src/test/java/com/fsck/k9/message/quote/TextQuoteCreatorTest.kt
            legacy/core/src/test/java/com/fsck/k9/message/TextBodyBuilderTest.kt
            legacy/core/src/test/java/com/fsck/k9/notification/AuthenticationErrorNotificationControllerTest.kt
            legacy/core/src/test/java/com/fsck/k9/notification/BaseNotificationDataCreatorTest.kt
            legacy/core/src/test/java/com/fsck/k9/notification/CertificateErrorNotificationControllerTest.kt
            legacy/core/src/test/java/com/fsck/k9/notification/LockScreenNotificationCreatorTest.kt
            legacy/core/src/test/java/com/fsck/k9/notification/NewMailNotificationManagerTest.kt
            legacy/core/src/test/java/com/fsck/k9/notification/NotificationActionServiceTest.kt
            legacy/core/src/test/java/com/fsck/k9/notification/NotificationContentCreatorTest.kt
            legacy/core/src/test/java/com/fsck/k9/notification/NotificationRepositoryTest.kt
            legacy/core/src/test/java/com/fsck/k9/notification/SendFailedNotificationControllerTest.kt
            legacy/core/src/test/java/com/fsck/k9/notification/SingleMessageNotificationCreatorTest.kt
            legacy/core/src/test/java/com/fsck/k9/notification/SingleMessageNotificationDataCreatorTest.kt
            legacy/core/src/test/java/com/fsck/k9/notification/SummaryNotificationDataCreatorTest.kt
            legacy/core/src/test/java/com/fsck/k9/notification/SyncNotificationControllerTest.kt
            legacy/core/src/test/java/com/fsck/k9/preferences/AccountSettingsUpgraderTest.kt
            legacy/core/src/test/java/com/fsck/k9/preferences/FolderSettingsUpgraderTest.kt
            legacy/core/src/test/java/com/fsck/k9/preferences/ServerSettingsUpgraderTest.kt
            legacy/core/src/test/java/com/fsck/k9/preferences/SettingsExporterTest.kt
            legacy/core/src/test/java/com/fsck/k9/preferences/SettingsImporterTest.kt
            legacy/core/src/test/java/com/fsck/k9/preferences/SettingsUpgradeHelperTest.kt
            legacy/core/src/test/java/com/fsck/k9/TestApp.kt
            legacy/core/src/test/java/com/fsck/k9/UnifiedInboxConfiguratorTest.kt
            legacy/mailstore/src/test/java/app/k9mail/legacy/mailstore/DefaultFolderQueryRepositoryTest.kt
            legacy/mailstore/src/test/java/app/k9mail/legacy/mailstore/folder/DefaultFolderDetailsRepositoryTest.kt
            legacy/mailstore/src/test/java/app/k9mail/legacy/mailstore/folder/DefaultRemoteFolderDetailsRepositoryTest.kt
            legacy/mailstore/src/test/java/app/k9mail/legacy/mailstore/folder/DefaultRemoteFolderQueryRepositoryTest.kt
            legacy/mailstore/src/test/java/app/k9mail/legacy/mailstore/folder/push/DefaultPushFoldersQueryRepositoryTest.kt
            legacy/mailstore/src/test/java/app/k9mail/legacy/mailstore/folder/push/DefaultPushFolderTrackingRepositoryTest.kt
            legacy/mailstore/src/test/java/app/k9mail/legacy/mailstore/MessageStoreManagerTest.kt
            legacy/storage/src/test/java/com/fsck/k9/preferences/StorageEditorTest.kt
            legacy/storage/src/test/java/com/fsck/k9/preferences/StoragePersisterTest.kt
            legacy/storage/src/test/java/com/fsck/k9/storage/messages/CopyMessageOperationsTest.kt
            legacy/storage/src/test/java/com/fsck/k9/storage/messages/DeleteFolderOperationsTest.kt
            legacy/storage/src/test/java/com/fsck/k9/storage/messages/DeleteMessageOperationsTest.kt
            legacy/storage/src/test/java/com/fsck/k9/storage/messages/MessageDatabaseHelpers.kt
            legacy/storage/src/test/java/com/fsck/k9/storage/messages/SaveMessageOperationsTest.kt
            legacy/storage/src/test/java/com/fsck/k9/storage/migrations/MigrationTo85Test.kt
            legacy/storage/src/test/java/com/fsck/k9/storage/migrations/MigrationTo86Test.kt
            legacy/storage/src/test/java/com/fsck/k9/storage/migrations/MigrationTo87Test.kt
            legacy/storage/src/test/java/com/fsck/k9/storage/migrations/MigrationTo88Test.kt
            legacy/storage/src/test/java/com/fsck/k9/storage/migrations/MigrationTo90Test.kt
            legacy/storage/src/test/java/com/fsck/k9/storage/migrations/MigrationTo91Test.kt
            legacy/storage/src/test/java/com/fsck/k9/storage/TestApp.kt
            legacy/ui/legacy/src/test/java/com/fsck/k9/activity/compose/AttachmentPresenterTest.kt
            legacy/ui/legacy/src/test/java/com/fsck/k9/activity/compose/MessageActionsTest.kt
            legacy/ui/legacy/src/test/java/com/fsck/k9/activity/compose/RecipientPresenterTest.kt
            legacy/ui/legacy/src/test/java/com/fsck/k9/activity/compose/ReplyToPresenterTest.kt
            legacy/ui/legacy/src/test/java/com/fsck/k9/activity/setup/AccountSetupCompositionViewModelTest.kt
            legacy/ui/legacy/src/test/java/com/fsck/k9/message/PgpMessageBuilderTest.kt
            legacy/ui/legacy/src/test/java/com/fsck/k9/TestApp.kt
            legacy/ui/legacy/src/test/java/com/fsck/k9/ui/messagelist/MessageListAdapterTest.kt
            legacy/ui/legacy/src/test/java/com/fsck/k9/ui/messageview/ViewIntentFinderTest.kt
            legacy/ui/legacy/src/test/java/com/fsck/k9/ui/settings/AboutViewModelTest.kt
            legacy/ui/legacy/src/test/java/com/fsck/k9/ui/settings/account/AccountSettingsDataStoreNotificationChannelTest.kt
            legacy/ui/legacy/src/test/java/com/fsck/k9/ui/settings/account/AccountSettingsDataStoreTest.kt
            legacy/ui/legacy/src/test/java/com/fsck/k9/ui/settings/export/SettingsExportViewModelTest.kt
            legacy/ui/legacy/src/test/java/com/fsck/k9/ui/settings/general/GeneralSettingsViewModelTest.kt
            mail/common/src/test/java/com/fsck/k9/mail/BoundaryGeneratorTest.kt
            mail/protocols/imap/src/test/java/com/fsck/k9/mail/store/imap/RealImapFolderTest.kt
            mail/protocols/imap/src/test/java/com/fsck/k9/mail/store/imap/RealImapStoreTest.kt
            mail/protocols/pop3/src/test/java/com/fsck/k9/mail/store/pop3/Pop3ConnectionTest.kt
            mail/protocols/pop3/src/test/java/com/fsck/k9/mail/store/pop3/Pop3FolderTest.kt
            mail/protocols/pop3/src/test/java/com/fsck/k9/mail/store/pop3/Pop3StoreTest.kt
            mail/protocols/smtp/src/test/java/com/fsck/k9/mail/transport/smtp/SmtpTransportTest.kt
        """.trimIndent().lines().toSet()
    }
}

internal fun mockitoUsageViolations(
    files: List<KoFileDeclaration>,
    projectRoot: String,
    allowlist: Set<String>,
): List<QualityViolation> {
    val root = File(projectRoot)
    val nonDiMockitoFiles = files.filter { file ->
        !file.path.endsWith("DiModuleTest.kt") && file.imports.any { it.name.startsWith("org.mockito.") }
    }.associateBy { File(it.path).relativeTo(root).invariantSeparatorsPath }

    val forbiddenImports = nonDiMockitoFiles.filterKeys { it !in allowlist }.values.flatMap { file ->
        file.imports.filter { it.name.startsWith("org.mockito.") }.map { declaration ->
            QualityViolation(
                location = declaration.location,
                reason = "Mockito import outside a DI test: ${declaration.name}.",
                suggestion = "Use a fake instead. Mockito is only allowed in *DiModuleTest.kt and existing exceptions.",
            )
        }
    }
    val staleExceptions = (allowlist - nonDiMockitoFiles.keys).map { path ->
        QualityViolation(
            location = root.resolve(path).absolutePath,
            reason = "Stale Mockito allowlist entry: $path.",
            suggestion = "Remove this exact path from existingMockitoUsers in ValidateMockitoUsage.kt.",
        )
    }
    return forbiddenImports + staleExceptions
}
