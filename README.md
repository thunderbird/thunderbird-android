Squeaky Mail
===

This is an email client derived from K-9 Mail, an advanced email client for Android. It is compatible with PGP KeyRing for Android, which is the purpose for it existing at all.

Only a few files were changed in a meaningful way. However, because an Android app has to have a unique package name, the "R.java" object now lives in a different package, necessitating changing the imports of a lot of source files. 



Meaningful file changes (that is, changes that incorporate integration with PGP KeyRing):

- crypto/CryptoProvider.java
- activity/setup/AccountSettings.java
- Account.java

The following files were added:
- crypto/PGPKeyRing.java



Resources that were changed:

- AndroidManifest.xml (to change the package name of the application)
- all strings.xml
- arrays.xml
- all icon.png (to replace the K9Mail icon with the Squeaky Mail icon)
- color_picker_dialog.xml



Source files whose imports were modified:

src/com/fsck/k9/Account.java
src/com/fsck/k9/EmailAddressAdapter.java
src/com/fsck/k9/K9.java
src/com/fsck/k9/activity/AccessibleEmailContentActivity.java
src/com/fsck/k9/activity/AccountList.java
src/com/fsck/k9/activity/Accounts.java
src/com/fsck/k9/activity/ActivityListener.java
src/com/fsck/k9/activity/ChooseFolder.java
src/com/fsck/k9/activity/ChooseIdentity.java
src/com/fsck/k9/activity/ColorPickerDialog.java
src/com/fsck/k9/activity/EditIdentity.java
src/com/fsck/k9/activity/EmailAddressList.java
src/com/fsck/k9/activity/FolderInfoHolder.java
src/com/fsck/k9/activity/FolderList.java
src/com/fsck/k9/activity/LauncherShortcuts.java
src/com/fsck/k9/activity/ManageIdentities.java
src/com/fsck/k9/activity/MessageCompose.java
src/com/fsck/k9/activity/MessageList.java
src/com/fsck/k9/activity/NotificationDeleteConfirmation.java
src/com/fsck/k9/activity/UnreadWidgetConfiguration.java
src/com/fsck/k9/activity/UpgradeDatabases.java
src/com/fsck/k9/activity/setup/AccountSettings.java
src/com/fsck/k9/activity/setup/AccountSetupAccountType.java
src/com/fsck/k9/activity/setup/AccountSetupBasics.java
src/com/fsck/k9/activity/setup/AccountSetupCheckSettings.java
src/com/fsck/k9/activity/setup/AccountSetupComposition.java
src/com/fsck/k9/activity/setup/AccountSetupIncoming.java
src/com/fsck/k9/activity/setup/AccountSetupNames.java
src/com/fsck/k9/activity/setup/AccountSetupOptions.java
src/com/fsck/k9/activity/setup/AccountSetupOutgoing.java
src/com/fsck/k9/activity/setup/FolderSettings.java
src/com/fsck/k9/activity/setup/FontSizeSettings.java
src/com/fsck/k9/activity/setup/Prefs.java
src/com/fsck/k9/activity/setup/WelcomeMessage.java
src/com/fsck/k9/controller/MessagingController.java
src/com/fsck/k9/crypto/Apg.java
src/com/fsck/k9/fragment/MessageListFragment.java
src/com/fsck/k9/fragment/MessageViewFragment.java
src/com/fsck/k9/helper/FileBrowserHelper.java
src/com/fsck/k9/helper/MessageHelper.java
src/com/fsck/k9/helper/SizeFormatter.java
src/com/fsck/k9/mail/internet/MimeUtility.java
src/com/fsck/k9/mail/store/ImapStore.java
src/com/fsck/k9/mail/store/LocalStore.java
src/com/fsck/k9/mail/store/StorageManager.java
src/com/fsck/k9/preferences/AccountSettings.java
src/com/fsck/k9/preferences/GlobalSettings.java
src/com/fsck/k9/preferences/IdentitySettings.java
src/com/fsck/k9/provider/UnreadWidgetProvider.java
src/com/fsck/k9/search/SearchAccount.java
src/com/fsck/k9/service/RemoteControlService.java
src/com/fsck/k9/view/AttachmentView.java
src/com/fsck/k9/view/MessageCryptoView.java
src/com/fsck/k9/view/MessageHeader.java
src/com/fsck/k9/view/MessageWebView.java
src/com/fsck/k9/view/SingleMessageView.java
