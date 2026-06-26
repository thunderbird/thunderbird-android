## K-9 Mail 6.x Release Notes

Historical change logs for K-9 Mail 6.x branches:

- [6.9xx Main (unstable) Development Branch](https://thunderbird.github.io/thunderbird-android/changelog_main_branch.xml)
- [6.8xx Branch](https://thunderbird.github.io/thunderbird-android/changelog_6.8-MAINT_branch.xml)
- [6.6xx Branch](https://thunderbird.github.io/thunderbird-android/changelog_6.6-MAINT_branch.xml)
- [6.4xx Branch](https://thunderbird.github.io/thunderbird-android/changelog_6.4-MAINT_branch.xml)
- [6.2xx Branch](https://thunderbird.github.io/thunderbird-android/changelog_6.2-MAINT_branch.xml)
- [6.0xx Branch](https://thunderbird.github.io/thunderbird-android/changelog_6.0-MAINT_branch.xml)

## K-9 Mail 5.x Release Notes

Historical change logs for K-9 Mail 5.x branches:

- [5.8xx Branch](https://thunderbird.github.io/thunderbird-android/changelog_5.8-MAINT_branch.xml)
- [5.6xx Branch](https://thunderbird.github.io/thunderbird-android/changelog_5.6-MAINT_branch.xml)
- [5.4xx Branch](https://thunderbird.github.io/thunderbird-android/changelog_5.4-MAINT_branch.xml)
- [5.2xx Branch](https://thunderbird.github.io/thunderbird-android/changelog_5.2-MAINT_branch.xml)
- [5.0xx Branch](https://thunderbird.github.io/thunderbird-android/changelog_5.0-MAINT_branch.xml)

## K-9 Mail 4.x Release Notes

## Older Releases

Release notes for earlier versions of K-9 Mail can be found below.

* [4.8xx Branch.](https://thunderbird.github.io/thunderbird-android/changelog_4.8-MAINT_branch.xml)  [_Note:_ The first two releases on this branch (4.800 & 4.801) will [only install and run on Android 4.4 (KitKat)](https://plus.google.com/114221735215588688400/posts/TBN86GgAAoX).  Later releases (4.802+) are compatible with Android 2.2+ (Froyo).]

* [4.590 Branch](https://thunderbird.github.io/thunderbird-android/changelog_4.6-MAINT_branch.xml) (This was intended to become the 4.6xx branch, but issues related to the release of Android 4.4 interrupted K-9 Mail's normal release process.)

## K-9 Mail 4.409

* Some longtime users of K-9 have reported incredibly slow message display times in 4.4. We haven't yet gotten our hands on a device that exhibits the problem, so we haven't been able to develop a proper fix. One thing that has worked for everyone who's tried it is to
  long-press on the account name in the account list and select "Recreate Data" -- This will COMPLETELY ERASE the local copy of your mail and redownload a brand new copy of everything from your server. Any mail that's on your device but not on your server will be DELETED.
* Super-dense message lists are now only shown if you set the number of message preview lines to 0 in the global display settings and disable display of contact pictures.
* New Slovak translation
* Updates to Czech, Spanish, Finnish, French, Korean, Polish and Russian translations.
* Labels for SSL/TLS and STARTTLS have been clarified.
* Added autoconfiguration for Fastmail.FM
* Tightened up the account list display for narrow devices

## K-9 Mail 4.408

* Restore K-9's super-dense message lists when contact pictures are disabled and the number of preview lines is set to 0.

## K-9 Mail 4.407

* Fixed a typo that made the changelog not visible on version 4.406

## K-9 Mail 4.406

* Add some Russian ISPs for autoconfiguration
* Performance improvements when opening messages and marking them as read
* Update the Play Store's URL
* Performance improvements to our message view
* Update the android-support library to the latest version
* Fix a bug where actions started from the message list context menu could act on the wrong message

## K-9 Mail 4.405

* Updates to Catalan, Chinese and Russian translations
* Workarounds for some broken POP3 servers
* Performance improvements

## K-9 Mail 4.404

* Fixed a bug that caused K-9 to crash on upgrade in certain limited circumstance
* Bugfixes for the new Folders menu item
* Fixes for several crashes reported on the Play store
* Tweaks to checkboxes and color chip display

## K-9 Mail 4.403

* Fixed a bug that caused K-9 to crash on the Accounts page if you're using the dark theme. Sorry!

## K-9 Mail 4.402

* Add additional shortcuts to the Folder list.
* Add an "empty trash" option to the Account context menu
* Fixes for several crashing bugs reported by Play Store users

## K-9 Mail 4.401

* Never use extended notifications when privacy mode is enabled
* Remove submenu from the account context menu to work a bug in some Galaxy devices
* Fixes for several errors reported by Play Store users

## K-9 Mail 4.400 (June 26, 2013)

## Miscellaneous

* Switched to a more holo-themed user interface
* Added a split screen setting to display message list and message contents side by side
* Added dialog to show the changes since the last update of K-9 Mail
* Added screen to notify the user of database upgrades after updating K-9 Mail
* Improve integration with third-party apps

## Notifications

* Added rich notifications on Android 4.1+ devices
* Changed the notification icon (Android 2.3+ only)

## Options

* Removed "Manage back button" setting. The back button now always goes to the previously displayed screen.
* Added new color picker to select account colors
* Unified some account settings into "General Settings" and "Reading Mail"
* Removed "compact layout" setting

## Composing mail

* Composing messages in right to left languages should now work better

## Forwarding mail

* Add References and In-Reply-To header for forwarded messages

## Reading mail

* Fixed inability to zoom out when viewing messages
* Added setting to automatically shrink messages to fit the screen width
* Improved display of messages decrypted with APG
* Removed "system zoom controls" setting (zoom controls will only be shown on devices that don't support pinch-to-zoom)
* Added work-around for the auto-scroll issue of the message view on Jelly Bean
* Moved "show all headers" into the menu of the message list
* Changed colors of the quote dividers

## Message list

* Added message threading (note: this is not the same as Gmail's conversation view)
* Added a setting to display contact pictures in the message list
* Changed from chips to colored circles that also indicate the (un)read state
* Added pull-to-refresh to check for mail
* Use different background for read messages in message list

## Folder list

* Improved last folder update time formatting
* Added support for wrapping long folder names in the folder list

## Widget

* Changed appearance of the unread widget
* Added support for "Unified Inbox" and "All messages" to unread widget

## Protocol support

* IMAP: Added support for server-side search (currently limited to sender and subject)
* IMAP: Added support for $Forwarded flag

## Notable bug fixes

* Added notification on SSL certificate validation failures
* Worked around ">" getting included in URLs when linkifying text/plain emails
* IMAP: Fixed SPECIAL-USE detection of Spam folder
* IMAP: Fixed a bug that caused messages to be lost when "moved" to a non-existant folder

## Localization

* Updated Japanese translation (Koji Arai)
* Updated Finnish translation (Jani Kinnunen)
* Updated Italian translation (Vincenzo Reale)
* Updated French translation (Erkan)
* Updated Greek translation (igavio)
* Updated Russian translation (Alexey)
* Updated Czech translation (Ondřej Vodáček, Jan Urbánek)
* Updated Hungarian translation (RootRulez)
* Updated Brazilian Portuguese translation (Marcio Zomb13)
* Updated Traditional Chinese translation (Leo Yang)
* Updated German translation (Danny Baumann)
* Updated Spanish translation (enriquefeas)
* Updated Dutch translation (Sander Bogaert)
* Updated Korean translation (Uni-)
* Updated Danish translation (l.meldgaard)
* Updated Catalan translation (Enric Morales)

## K-9 Mail 4.200 (September 4, 2012)

## Initial Setup

* Automatic folder config for Yahoo!, Gmail, and other RFC6154 SPECIAL-USE-compliant servers
* Set the IMAP namespace for Gmail during auto-configuration
* Changed UI to allow selecting the empty IMAP namespace
* Set 'When I delete a message' to 'Delete from server' by default for IMAP on automatic setup like manual setup does
* Set default names for Archive and Spam on manual config like automatic setup does
* Added autoconfiguration settings for several providers

## Options

* Option to have confirmation on deleting starred messages
* Option to strip signature from quoted text in replies
* Extended setting to hide subject in notification area (never, when locked, always)
* Save settings changes "on pause" rather than "on back" so we actually save settings when a user hits home or menu or whatever
* Added font size setting for text input fields in the message composition screen
* Added a setting to disable marking messages as read on viewing
* Enable system zoom and disable gestures by default
* Add discard confirmation dialog when pressing back in message compose
* Option to now refuse to save drafts of messages marked as encrypted

## Composing mail

* Added a button to switch the identity in the message composition screen
* Prompt user which email address to use when a contact with multiple email addresses was picked

## Replying to mail

* Fixed quoting of text/plain reply when sending text/html messages (builds plain reply separately instead of converting from the HTML)
* Forwarded messages always use the quoting style 'Header'
* You can now can have reply and signature before quote, reply and signature after quote, or reply before and signature after quote (setting reply after and signature before still corrects to both after)
* Convert quote characters in plain text messages to blockquotes in the HTML version.  "Be like Thunderbird."
* Colorize quoted text that has been converted to HTML.  Like Thunderbird, but optimized for mobile (no right margin quote bar, thinner left margin quote bar).
* Added 'From:' addresses on "reply all" even when 'Reply-To:' is set. Don't repeat address in 'To:' field if it's already included because it's the 'Reply-To:' address.

## Reading mail

* Preserve header order (with the exception of duplicate headers like 'Received' they all show up where the first one did)
* Improved zooming/scrolling when viewing a message
* Added long-press popup menu to links in email message display
* Workaround to detect the charset from HTML mail without charset parameter on the header
* Added button to show unnamed and inline attachments

## Message list

* Added batch buttons for 'Archive' and 'Move'. Made batch buttons configurable
* Remember the most recent sort setting in the message list
* Added a "sort by arrival" option

## Widget

* Added widget to display the unread count for an account
* Added launcher shortcuts for special accounts/folders ("Unified Inbox" and "All messages")

## Protocol support

* Fixed the parsing of response data for WebDAV requests
* Fall back to HELO command if the server doesn't know EHLO (SMTP)
* Improvements to mail sending timeout issues
* Yahoo! IMAP seems to no longer require 'ID ("GUID" "1")' over non-3G.  See http://mobile.yahoo.com/mail for IMAP setup instructions.

## Platform compatibility

* Support ACTION_SEND extras when handling ACTION_SENDTO. Too many applications get this wrong and rely on the undocumented behavior of the AOSP Email app and Gmail

## Notable bug fixes

* IMAP message UIDs are 32 bit unsigned values and cannot be stored in int, so we now parse them as long.
* Disable special screen reader mode on ICS. The default message view has special accessibility features already.
* Don't save to drafts if drafts folder is -NONE-
* Hide buttons & settings that don't do anything on POP3 accounts
* Delete attachment metadata and thumbnails when deleting attachments
* Reset unread/flagged count on "empty trash"

## Localization

* Updated French translation (erkan2005)
* Updated Japanese translation
* Added Hebrew translation (elyashiv sabach - אלישיב סבח)
* Updated zh-rTW translation (Jeff117077)
* Updated Polish translation (Błażej Jeżewski)
* Added Greek translation (IGaviotis)
* Updated Turkish translation (wakeup)
* Added Danish translation (mittaa)
* Updated French and German translation (damor)
* Updated Russian translation (Dmitry Ashkadov)
* Added Ukrainian translation (Andriy Kopanytsia)
* Updated Spanish translation (AntonioGPS)
* Updated Chinese (zh-CN) translation (sunnavy)
* Updated Finnish translation (jkinnunen)
* Updated Italian translation (Votyer)
* Work around a bug in Android 4.x (?) when the system locale is Turkish

## K-9 Mail 4.009 (May 8, 2012)

* K-9 4.009 fixes a catastrophic error that could cause mail stored on SD cards to vanish. We're really, really sorry we didn't catch this before 4.008 shipped.

## K-9 Mail 4.008 (May 7, 2012)

* K-9 4.008 fixes a regression on Android 2.1 or earlier introduced in 4.007.

## K-9 Mail 4.007 (April 28, 2012)

* K-9 4.007 includes a fix for a permissions issue on K-9's data files discovered (and fixed) by Ashley Willis.

## K-9 Mail 4.006 (February 15, 2012)

* Upgraded the "read messages" and "delete messages" permissions from "normal" to "dangerous" to allow users and devices to better manage user data security.
* Fixed a rare NPE in autocompletion of email addresses.

## K-9 Mail 4.005 (January 27, 2012)

* Fixed "mark as unread" from message view (Issue 3958)

## K-9 Mail 4.004 (January 20, 2012)

* Fixed problem when importing multiple accounts
* Fixed bug that caused drafts to be lost under certain circumstances (Issue 3696)
* Fixed importing account settings for IMAP accounts (Issue 3986)

## K-9 Mail 4.003 (January 8, 2012)

* Fix the swipe detection algorithm in message list. (Issue 3875)
* Fix crash when deleting an account. (Issue 3954)

## K-9 Mail 4.002 (January 2, 2012)

* Show/hide ListView footer depending on message list type (Fixes issue 3946)
* Added new Danish translation

## K-9 Mail 4.001 (December 26, 2011)

## Bug fixes

* Fixed a bug that stopped WebDAV support from working for many users

## K-9 Mail 4.000 (December 24, 2011)

## User Interface

### Account List

* Added ability to change the order of accounts ([Issue 1251](http://code.google.com/p/k9mail/issues/detail?id=1251))
* Fixed counting of new unread messages

### Folder List

* Dynamically update the titlebar when the unread state of a listed folder changes ([Issue 3177](http://code.google.com/p/k9mail/issues/detail?id=3177))
* Respect "move/copy destination folders" when selecting a target folder

### Message List

* Search same sender from searchable folders instead of the unified inbox
* Switch subject to marquee ellipsis for landscape layout
* Properly keep selection in case of a configuration change (orientation, keyboard, etc.) during user interaction (e.g. a dialog)
* Reset visible limit when "clear messages" is used

### Viewing Attachments

* Improved support for picky/buggy apps when viewing attachments
* Rewrite broken MIME types to standard values (e.g. image/pjpeg to image/jpeg)

### Composing Messages

* Added support for requesting read receipts (Alessandro Russo)
* Added button to re-add the quoted text editbox after it has been removed
* Show (and populate) CC field when using "Reply all" on a message with recipients in the CC field ([Issue 3509](http://code.google.com/p/k9mail/issues/detail?id=3509))
* Fixed restoring drafts saved with "Reply after quoted text" enabled ([Issue 2568](http://code.google.com/p/k9mail/issues/detail?id=2568))

### HTML Mail

* Fixed bug with generation of HTML replies where no text was quoted
* Fixed bug where message was HTMLified when sent without quoted text but should have been sent as plain text ([Issue 3530](http://code.google.com/p/k9mail/issues/detail?id=3530))

### MIME Internals

* Explicitly use Locale.US when dealing with dates in message headers
* Fixed parsing of messages with a very large header
* Make sure disposition size parameter is a valid number before using it ([Issue 3712](http://code.google.com/p/k9mail/issues/detail?id=3712))
* Don't crash on messages without valid From-header ([Issue 3683](http://code.google.com/p/k9mail/issues/detail?id=3683), [Issue 3814](http://code.google.com/p/k9mail/issues/detail?id=3814))
* Fixed bug where invalid MIME boundaries could be created when using locales with difficult uppercasing rules (e.g. turkish) ([Issue 3733](http://code.google.com/p/k9mail/issues/detail?id=3733))

### Configurability

* Added settings import/export
* Added account setting to configure whether or not to quote the original message by default
* Option to show next message after delete by default instead of previous
* Added 2MB, 5MB and "All" message download options
* Loosened the restrictions on the TLD to support custom private TLDs, as well as single label hostnames ([Issue 2784](http://code.google.com/p/k9mail/issues/detail?id=2784)) (rpimonitrbtch)

### Internationalization

* Added Turkish translation (Recep PAYVERDI)
* Added Hungarian translation (Sándor Péter Nagy)
* Updated Catalan translation (dvbotet)
* Updated Dutch translation (Jan-Bart)
* Updated French and French Canadian translation
* Updated German translation
* Updated Japanese translation
* Updated Korean translation
* Updated Russian translation (Alexei Emanov)
* Updated Taiwan Traditional Chinese translation (Jeff117077)

## Protocol support

### WebDAV (Exchange)

* Disabled "download by date range" which caused messages outside the date range to be downloaded over and over
* Fixed bug where UID in response was truncated (Adam)
* Fixed some crashes

### POP3

* Disabled "download by date range" which caused messages outside the date range to be downloaded over and over
* Don't open a connection to the POP3 server if a message is marked as read (since that's a local operation)
* Implemented work-around to handle malformed UIDL responses ([Issue 3546](http://code.google.com/p/k9mail/issues/detail?id=3546))

### SMTP

* Treat 5xx SMTP return codes as permanent failures

## Miscellaneous

### SSL/TLS

* Don't crash when trying to connect to an non-SSL service using SSL ([Issue 3798](http://code.google.com/p/k9mail/issues/detail?id=3798))

### Checking mail

* Work around cases where mail was last checked in the future

### Permissions

* No longer suggest a display name based on the accounts configured in Android. Accordingly, no longer request the permissions `android.permission.GET_ACCOUNTS` and `android.permission.READ_OWNER_DATA`

### Services

* Stop service(s) if not in use ([Issue 2777](http://code.google.com/p/k9mail/issues/detail?id=2777)) (Christian Knecht)

## For developers

### Message Provider

* Added field UNREAD (João Pedro Taveira)
* Added field for sender address ([Issue 3665](http://code.google.com/p/k9mail/issues/detail?id=3665)) (Chris Banes)

## K-9 Mail 3.x Release Notes

## K-9 Mail 3.803 (September 15, 2011)

## Bug Fixes

* Fixed a bug that caused two Outbox folders to be displayed

## K-9 Mail 3.802 (June 9, 2011)

## Bug Fixes

* Fixed a bug that caused certain email messages to show up as "No Text"

## K-9 Mail 3.801 (May 26, 2011)

## Bug fixes

* NPE fix in message list
* Restore support for Android 1.5

## K-9 Mail 3.800 (May 26, 2011)

## User Interface

## Account List

* Unavailable accounts are now greyed out

## Folder List

* You can now start typing to filter K-9's folder list

## Message List

* Fixed display of colorized name in the message list.
* The date format preference is now refreshed each time the messages get listed.  Previously, changes to the date format preference weren't noticed until the app restarted.
* Mark messages as ANSWERED only upon send.

## Message Display

* JPEG images with the MIME type "image/jpg" now open correctly on Motorola devices.
* Where a message has attachments, a long press on the "Save" button will allow the user to choose where to save the attachment to using either a file browser or by specifying a path; a short press saves to the default location (see "Configurability" below). Supported file browsers are OI File Manager, AndroidWorkz SystemExplorer, ES File Browser and Blackmoon File Browser (issue 1027).
* Fixes for downloading attachments

## Composing Messages

* Added an addressbook lookup button
* Explicitly set focus to "To:" input field when composing or forwarding a message.
* Made it possible to BCC to more than 1 address

## About box

* Fix display of icon in About box on Android 2.1 and earlier

## HTML Mail

* Use pre instead of div so that plain text clients get line wrapping correct.

## MIME Internals

* Correct encoding and decoding for the filename parameter in the Content-Disposition.
* Should store the disposition-type without Content-Disposition parameters.

## Configurability

* Implemented option to hide special accounts (issue 3060)
* Added option to disable confirmation dialog for "mark all as read" (issue 1415)
* Added "move to spam folder" confirmation dialog to MessageList
* Added "Save attachments to..." option to allow a default location for saving attachments (issue 1027)

## Internationalization

* Updated Brazilian Portugese translation from noeczisto
* Added Catalan translation from David Valls
* Added a Galician (Galego) translation from fernando.coello@gmail.com
* Updated Chinese translation update from sunnavy
* Updated Finnish translation from Patrik Selin
* Updated Italian translation with patch provided by paolo.maccione
* Updated Japanese translation
* Added a Korean translation (from sungguk)
* Updated Polish translation from alekhavoc and rabbbit
* Updated Swedish translation from daniel.malmgren.DM
* Updated Spanish translation with changes provided by Hayawata
* Added support for emoji input in message subjects (on Japanese Devices only)
* Improved support for iso-2022-jp messages

## Protocol support

## IMAP

* Remove trailing space from IMAP command as a potential fix to issue 3089.
* Fix length-check of BODYSTRUCTURE reply; fixes issue 3116.
* Unify two IMAP date parsing paths, improving the exception so we can learn what date format we don't cope with.
* Added another badDateTimeFormat to ImapResponseParser to handle IMAP servers that generate timestamps without a timezone.  Fixed issue 3179.
* Fix bug with partitioning of IMAP FETCH commands

## WebDAV (Exchange)

* In getPersonalNameSpaces now we ask the server for the real folder names corresponding to the special folders ids (for which we have constants), so we can map the account folders using the real names.
* Support non-English Exchange installations.
* Fix the ability to accept invalid SSL certificates for exchange support.

## POP3

* Made the minor modifications to support CRAM-MD5 authentication in POP3 accounts.
* Disable "Enable refile buttons" preference for POP3 users

## SMTP

* New acounts now default to using the SMTP submission port (587) rather than the regular server-to-server (25) transport port.
* Permanent failures when trying to deliver a message move it back to drafts.
* Tempfail outgoing messages after 5 delivery attempts
* Opening a "permfail" message takes you to the drafts folder
* Split out "tempfail" and "permfail" message delivery errors
* Fix "sending message failed" notifications to actually report the server error
* If an SMTP server tells us how big the biggest message they accept is, don't even try to deliver a message to them if it's bigger than that size
* For a while, it was possible for users to configure their outbox and drafts folders to be the same folder. This could result in drafts being sent over and over.  We now use the K-9 Identity header as a shibboleth for drafts, as it's not actually set when a message is sent.
* Removed some extra spaces from SMTP commands which picky servers rejected.  Fixes issue 3182.
* Add options for PLAIN and LOGIN authentication even when not advertised by the server.  Legacy PLAIN autoselection behavior is retained.

## For developers

## Attachment Provider

* AttachmentProvider can now return an attachment with an unmodified MIME type. However, when viewing attachments this is not desirable. So we try hard to return a sensible MIME type (use extension to look up a MIME type if it's originally "application/octet-stream"; or replace with canonical MIME type if it's known to be wrong, e.g.  "image/jpg" -> "image/jpeg")

## Sharing Messages

* Add extra for the sender address(es) to the "Share intent"
* Get rid of the "Fwd: " prefix when sharing a message. Add recipients to the intent ("To:" = EXTRA_EMAIL, "CC:" =  EXTRA_CC)

## Portability

* Change our technique for sending \r\n at the end of smtp commands to deal with a weird failure on honeycomb

## Testing

* We have a test suite that can run (thanks to achen's start)

## Build Infrastructure

* Added an 'astyle' build target
* Switched the code's style back to something resembling AOSP standards
* switch build infrastructure to git
* Restore tools/build-beta to its former glory. It can now build a "K9 beta with different keys" that can sit next to an installed K-9.
* K-9 now compiles in the libs we only need at build time when building with eclipse.  This is an annoying default, but it'll make life easier for newbies starting out with K-9 for eclipse.  We should ALWAYS do release builds with ant

## Bug fixes

* Fixed polling schedule after SD cards are mounted or unmounted
* Fixed several NullPointerExceptions

## Performance

* Destroy all messages in one SQLite transaction
* Perf improvement: use parcels instead of serializable. Standard Java serialization is slow on Android. Replacing it w/ parcelable makes it around 10x faster (on a N1, with ~ 500 messages in the list).

## Third-party libraries

* Replaced our local copy of jzlib with a clean jar built from jzlib 1.0.7 downloaded from jcraft: http://www.jcraft.com/jzlib/jzlib-1.0.7.tar.gz
* Replaced local version of jutf7 with a homebuilt jar from:
* Replaced Apache Commons IO with v2.01 downloaded from apache.org as a prebuilt JAR
* Tell eclipse to bundle JARs in libs but NOT to bundle libs in compile-only-libs. (Results in Eclipse building something much closer to what ant does)
* Start building using a JAR of MIME4J
* Add support for building with precompiled JARs
* Removed dependency on (external) commons-codec and commons-logging
* Get rid of now unnecessary BouncyCastle jar
* Use SslCertificate that is part of the official Android API instead of BouncyCastle's X509Name (which is not shipped anymore in Android 3.0) to verify domain names on SSL/TLS connections.
* Use java.util.zip for inflating (Can't use it for deflating since it doesn't have Z_PARTIAL_FLUSH)

## K-9 Mail 3.604 (March 8, 2011)

## Sending Mail

* When an SMTP server sends a "permanent failure" error, K-9 stops trying to send the message
* When K-9 fails to deliver a message 5 times in a row, K-9 will now wait until the app stops and starts before trying again
* Error messages sent by SMTP servers should now actually be shown to the user in the Android Notification

## IMAP

* Slightly correct an IMAP command that made certain cranky servers cranky

## K-9 Mail 3.603 (February 27, 2011)

## User Interface

* Explicitly set focus to "To:" input field when composing or forwarding a message.

## Sending Mail

* Don't double-quote empty strings in From addresses to deal with certain cranky servers

## Connectivity

* Use SslCertificate that is part of the official Android API instead of BouncyCastle's X509Name (which is not shipped in Android 3.0) to verify domain names for SSL/TLS connections.

## Exchange

* Several protocol fixes

## IMAP

* Fix a bug with partitioning of IMAP FETCH commands

## SMTP

* Change our technique for sending \r\n at the end of SMTP commands to deal with a weird failure on honeycomb

## K-9 Mail 3.601 (February 7, 2011)

## Composing Mail

* Add button to allow editing of quoted message in HTML mode by dropping user back to text mode.

## K-9 Mail 3.600 (February 2, 2011)

## Settings

* Restore "only vibrate once" vibration notification option.
* Add a setting to enable Outlook-style message quoting.
* Add a setting to allow users to always show email addresses instead of the "friendly" parts of email addresses.
* Add a setting to disable unread count in notification bar.
* Add a setting to change the font size of the message preview in the message list.  Fixes [issue 2788](http://code.google.com/p/k9mail/issues/detail?id=2788)

## User Interface

* Round account color chips
* Be a little more graceful when scrolling horizontally in a (vertical) scroll view.  Not quite to the point of diagonal scrolling, but hopefully closer.
* Visual cleanup to Message Lists, Message views
* Stop showing "Not polling" in the status header. Most of the time, this is because Push mail is enabled.
* Improve the first page of the wizard on tablet-scale devices

## I18N

* Added a Brazilian Portugese translation from Marcio Viterbo
* Updated Italian translation from Giuseppe Arrigo and Paolo Maccione. Fixes [issue 2778](http://code.google.com/p/k9mail/issues/detail?id=2778)
* Updated Czech translation from lubekgc. Fixes [issue 2808](http://code.google.com/p/k9mail/issues/detail?id=2808)
* Updated German translation with slightly modified version of the patch provided by OliverMe...@googlemail.com
* Updated Italian translation from paolo.maccione
* Updated Spanish translation from Adolfo Gutiérrez Ocaña
* Improved emoji support for a wide variety of carriers. HIRANO Takahito
* Support for emoji in message subjects. HIRANO Takahito
* Allow emoji input on Japanese devices. HIRANO Takahito
* Implement phonetic search of Contacts on Eclair and earlier.

## Internals

* Upgrade to a newer version of MIME4J to gain significant performance improvements from the past few years of development
* Enable Strict Mode when a new "developer mode" is enabled and we're running on 2.3 or newer
* Update URL regexes by importing from AOSP and then from IANA
* Initial implementation of folder attributes on the folder, rather than in preferences
* Improve generation of plain text versions of HTML mail
* Improve generation of message previews

## Performance

* Improve performance of account statistics generation.
* Call the routine to convert emoji to images only when a message actually contains emoji.
* Batch storing of unsynced messages to speed up DB update (chunk size set to 5).
* Execute LocalMessage.appendMessage() & LocalMessage.setFlag() in the same transaction for small message storing in order to speed up DB update.

## Security

* Disable webview cache and javascript.

## Notifications

* When mail is sent successfully, cancel the "couldn't send mail" notification
* Don't notify for new mail in a designated spam folder
* Prevent new mail notifications for IMAP messages older than our most recent message.
* Use a heuristic to try to avoid notifying POP3 users about older mail messages

## Sending mail

* Preserve HTML formatting when replying to or forwarding HTML mail
* Clean up the display of quoted messages on the mail composition screen.
* When editing a previously saved draft, only show the BCC field if it has entries other than the auto-bcc for that account.
* Switch from generating X-User-Agent to User-Agent headers.  Fixes [issue 1917](http://code.google.com/p/k9mail/issues/detail?id=1917)

## Account setup

* Use full email addresses as usernames for alternate Yahoo! domains.
* Add support for other Yahoo! domains (ymail.com, rocketmail.com)
* Set up some reasonableish defaults for which folders to sync when creating a new account.

## IMAP

* Correctly encode/escape strings when used in IMAP commands.  Fixes [issue 2832](http://code.google.com/p/k9mail/issues/detail?id=2832)
* Escape backslashes in IMAP mailbox names
* Fix for the "K9 skips every 101st message when fetching on IMAP" bug by e-t172 <e-...@akegroup.org>. Fixes [issue 2819](http://code.google.com/p/k9mail/issues/detail?id=2819)
* Better handle the case where a list in an IMAP response is prematurely ended by CRLF. Fixes [issue 2852](http://code.google.com/p/k9mail/issues/detail?id=2852)

## SMTP

* Changed SMTP code to handle reply codes without additional text. Fixes [issue 2801](http://code.google.com/p/k9mail/issues/detail?id=2801)
* Try using IP addresses for EHLO if the local hostname is unavailable. Only use a default hostname if the IP address is unavailable. ref r2958, [issue 2750](http://code.google.com/p/k9mail/issues/detail?id=2750).

## Exchange

* Fix usage of 'advanced' exchange settings.
* Fix form based re-authentication when logon cookies had expired.

## Behavior

* Don't reset visible limits every time K-9 is opened, only when the user changes how many messages they want to be synced.

## K-9 Mail 3.403 (December 2010)

## Internationalization

* Italian translation updates from paolo.maccione
* Fix for a settings crash in Italian locales

## K-9 Mail 3.402 (December 2010)

## Internationalization

* Dutch translation updates from thayoung0
* Italian translation updates from paolo.maccione

## User Interface

* Slightly better padding around checkboxes in the touchable view
* When mail is sent successfully, cancel the "couldn't send mail" notification

## Performance

* Execute .appendMessage() & .setFlag() in the same transaction for small messages to speed up DB update.

## Stability

* Fix a couple NPEs

## Exchange

* Fix several Exchange authentication and reauthentication bugs.

## K-9 Mail 3.400 (December, 2010)

## Internationalization

* Updated Spanish translation from javier.crespocarreno and bergonzini
* Updated German translation from bobo_bb, therossoblu
* Updated Japanese translation
* Updated French translation

## Sending mail

* Added a first pass implementation of being able to resend a message.
* Try much harder to send outgoing mail
* Better notifications when message sending fails
* Replying to a message you sent now sends mail to the recipients of the original message, not to you.
* Relabeled the "Choose Identity" menu item in message compose to be a bit more obvious to end users.
* Display name and email address in separate lines for auto-complete results in message compose activity.  Fixes issue 2442
* Match beginning of words when searching for contacts in Android 1.5/1.6 (e.g. "Doe" will find "Doe, John" and "John Doe") Fixes issue 1354
* Mark contacts as contacted when a mail is sent to them.  Fixes issue 2522

## User interface

* Imported icon updates from Pierre-Luc Auclair
* Animated the batch-operations toolbar (Imported from AOSP)
* Pinch-to-zoom should now be more reliable. Patch by achen.code. Fixes isue 2720
* In Message Lists, unread message senders are now more consistently shown in bold.  patch from achen.code.  Fixes Issue 2719

## Message Storage

* Implement the ability to store messages on SD. You can find the toggle in Account Preferences under Storage.
* Fixed significant bugs in how we garbage-collect deleted messages
* Improved the "Do what I mean" nature of "Empty trash" - it now syncs the trash folder after deletions to clear out local records of deleted messages and does a compact operation on the local store.
* Try to gracefully handle account DB corruption by deleting file on open exception. Updates issue 2537
* Added the ability to completely wipe the locally cached messages of a single syncced folder.

## Settings

* preliminary implementation of 'quiet time'
* Replaced our color picker with a variant of android-color-picker
* Added a "fetch all messages" visible limit
* Added a new preference to let users select how many lines to show in message-preview mode.
* Moved our "pick a folder" preferences out of the account setup wizard, into the "Folders" section

## Crashes Fixed

* Fixed an NPE when toggling "batch" update buttons.
* Fixed an NPE in account shortcuts
* Guard against null content from html parts on reply/forward, which was causing a bunch of NPEs on the market.

## IMAP

* We don't cache positive DNS lokups. We shouldn't cache negative DNS lookups either. (Should be extended to all protocols)

## POP3

* Ignore messages without unique-id when parsing UIDL response Fixes issue 2731

## Exchange / WebDAV

* Exchange authenticaton and setup overhaul from Kris Wong
* Applied WebDavStore_cleanup.diff to remove dead code in the WebDav store from Kris Wong

## API

* Changed external intent (e.g. Intent.ACTION_SEND_TO) handling code in MessageCompose
  * only allow mailto URI for ACTION_VIEW and ACTION_SENDTO (as documented by AOSP)
  * allow additional recipients via "to" parameter in mailto URI
  * removed check for allowed MIME type when adding attachments since we're accepting all MIME types anyway
  * show CC and BCC text fields when one of those recipient types was set by the intent
  * use MIME type specified in intent for attachments (if present and doesn't contain a "*")

## Internals

* No longer delete the Outbox every time it's empty
* Significant automated code-cleanup from IntelliJ
* Improvements to message preview heuristics
* Fix for "http:/"-links in plaintext -emails. (add http:// is a link has no schema)

## K-9 3.207 (November 2010)

## Bugs Fixed

* Force close when returning to message list

## K-9 3.206 (November 2010)

## Bugs Fixed

* Force close while composing messages
* Crash while viewing certain HTML messages on Android 2.1 (Eclair.1)

## K-9 3.205

* Not released

## K-9 3.204 (October 2010)

## Bugs fixed

* Fixed Force closes on Android 1.5 devices

## Internationalization

* Updated Japanese Localization

## K-9 3.203 (October 2010)

## Syncing Mail

* K-9 now tries to send pending messages whenever we refresh pushers.  This should help with "stuck" unsent messages on push-only accounts.

## Internationalization

* Updated French Localization

## Bug fixes

* Fixed a bug that caused display of some HTML messages on Android 1.6-2.1 devices to fail
* Never show an undismissable  "new email" notification for a null account (like used to happen during setup)
* Unify two codepaths that sent unsent messages, one of them without a notification
* The "viewport" hinting we used to try to better fit messages by default made zooming incredibly unreliable. Now it's only somewhat unreliable.
* Bugfixes for contact colorization in message lists.

## K-9 3.202 (October 2010)

## User Interface

* Add gmail-style "to me" and "cc me" indicators in messagelist
* Previews for HTML-only messages
* Improved accessibility for Next/Previous buttons in message lists

## Performance

* When returning from a message view to a message list do our listview updates on a thread rather than on the main UI thread. This results in the list blinking with old data, but that's still a better user experience than the "frozen" behaviour in 3.200
* When doing a message search, break it into "first 10" and "more" - because a long SQL query takes more time before starting to return results, K-9 was increasingly slow on large folders.
* Workarounds for out-of-memory errors when HTMLifying plaintext messages.

## Attachments

* Updated MIME Type list with http://www.stdicon.com/mimetypes
* It turns out that not every android release has a good extension to mimetype mapping. restore our previous hardcoded mime database as a fallback
* Fix for NPE when changing identities

## Internationalization

* Simplified Chinese translation -- zhangyin@research.neu.edu.cn

## K-9 3.200 (October 2010)

## Features

* The IDEAL Group have joined the K-9 dogwalkers and submitted their code to give K-9 suppport for screenreaders to be part of K-9 -- jessev
* K-9 can now display Softbank, DoCoMo and KDDI Emoji (with the exception of 3 kddi glyphs that overlap with softbank).  -- jessev
* BETA version of a new MessagingProvider to enable K-9 widgets. API MAY CHANGE IN FUTURE RELEASES -- stephane.lajeunesse, koxx33 and fiouzy

## Selected Bug Fixes

* Numerous internal performance and memory efficiency improvements -- everybody
* Fixes for several storage "leaks" that could cause K-9 mailboxes to grow quite large over time -- jessev
* Improvements in HTML mail forwarding -- jessev
* Word wrap quoted text in replies. -- cketti
* Remember "Show pictures" button state when changing orientation. -- cketti
* Try harder to make message loading feel responsive and scroll correctly. -- jessev
* Don't show K9mail-errors as move/copy destination folder. -- cketti
* Improved accuracy in to "more from this sender" -- jessev

## User Interface ##

* "Integrated Inbox" is now "Unified Inbox" - it's a bit shorter, slightly more descriptive and it matches iPhone terminology. -- jessev
* "Forward with another app" is now "Share" -- jessev
* "Preferences" is now more consistently "Settings" -- jessev
* New mail alerts automatically open the folder they're alerting about.  Patch from thardie -- jessev
* Restore message list state when exiting message view -- jessev
* Keyboard filters on folder-selection -- Marcus.Wolschon@googlemail.com
* K-9's theme has been disabled within the Settings UI to work around an android bug. -- jessev
* Add support for voice search -- jessev

## Settings ##

* Significant overhaul of settings screens -- jessev
* New settings
  * Pinch-to-zoom messages -- jessev
  * Single-column layout for messages -- jessev
  * When to automatically show images in messages --jessev
  * Volume key navigation in MessageView, MessageList and all K9ListActivity subclasses.  Based on a patch from paulkilroy@gmail.com. -- jessev
  * Show names from contacts -- jca02266, cketti
  * Top- or Bottom-post replies.  Code By achen.code@gmail.com -- jessev
  * Disable LED notification for new messages -- fiouzy
  * Hide message subject in notification bar when device is locked -- fiouzy
  * Delete confirmation in message views -- fiouzy

## Internationalization ##

* Added support for country/region specific locales -- fiouzy
* Remove German version of the "Re:" prefix when replying to messages. So instead of "Re: AW: subject" we get "Re: subject". -- cketti
* New Canadian French translation, Pierre-Luc Auclair -- fiouzy
* New Dutch translation from Stephan Vijfhuizen -- jessev
* New Finnish translation from Ari Arokoski. -- fiouzy
* New Swedish translation from daniel.malmgren.DM / swedroid.se folks -- fiouzy
* Updated German translation by therossoblu, cketti -- cketti
* Updated French translation by Pierre-Luc Auclair, Antoine Aflalo, fiouzy -- cketti
* Updated Japanese translation -- jca02266

## Protocols ##

* Implement windowing for IMAP UID FETCH, improving K-9's performance and  stability on very large IMAP mailboxes -- jessev
* Use the IP address for SMTP EHLO if the current hostname contains invalid characters, e.g. underscore.  Fixes issue 2143 -- cketti
* Work around malformed UIDL responses (POP3).  Fixes issue 2252 -- cketti
* Don't open unneeded connections to the server when the protocol doesn't support flags. This avoids unnecessary connections to POP3 servers.  Fixes issue 2046 -- cketti

## K-9 3.0 (August 13, 2010)

## Major new features ##

* added launcher shortcuts to accounts, refs issue 1025 - thialfihar
* Support for inline PGP digital signatures and encryption with APG - thialfihar
* Support Emoji for DoCoMo.  Emoji icons are derived from http://start.typepad.jp/typecast/ - jca02266
* Added menu option to select & copy text in the message view. - cketti
* Allow users to choose an outgoing identity from any account during message composition - fiouzy & cketti
* First pass at a "download the rest of this message" button - jessev
* Added  optional "Archive" and "Spam" buttons to Message View - thialfihar

## Internationalization ##

* Added Czech localization provided by lubekgc (Thanks!) - cketti
* Added japanese translation provided by hatty. - jca02266
* Updated french translation (thanks, p.lucauclair) - cketti
* Complete French translation added. Cheers, plauclair. - thialfihar
* Updated polish translation with patch provided by borszczuk (see issue 1321) - cketti

## Build system ##

* Google got rid of the android-6 distribution in the SDK. We now build against android-8, (though we're still backwards compatible to android-3.) - jessev

## Selected internal improvements ##

* Use of constants for immutable empty arrays (prevent useless memory allocation) - fiouzy
* Try to avoid a bunch of reallocations as we grow the temporary output stream by defining its size if we can get it - jessev@gmail.com
* Backport an update to getTextFromPart from AOSP as of 290348 - dealing with OOM as we import large messages - jessev@gmail.com
* Switch from the incomplete (but compatible with java 1.3) mime4j charset alias resolver to the core java nio version with a better charset database; rather than falling back to not showing the user a failing message, fall back to showing the user the message as ascii -- it makes it possible to get _something_ out of an unknown-charset message, rather than just a "ha ha. no." error. - jessev
* use "http://" as baseUrl in WebView instead of "email://", refs issue 267 - thialfihar
* Better handling of cases where a special folder is set to INBOX (e.g. Sent folder = INBOX). Make sure we count unread messages and don't exclude the Inbox from notifications in this case.  Fixes issue 1817 - cketti
* Synchronize iterator accesses to mAdapter.messages to avoid ConcurrentModificationException in MessageList. Fixes issue 1598 - fiouzy
* Avoid NullPointerException when trying to move a message before it is fully loaded (in message view).  Fixes issue 1957 - cketti
* When counting remote unseen/flagged messages, don't window to only the most recent 300 messages - Since we display this # to the user, the "last 300" heuristic ends up confusing and _wrong_ on a regular basis. - jessev

## Configurability ##

* added a setting for language, overriding the system locale settings - thialfihar
* Add a per-account setting to choose how much of each message to download automatically. - jessev
* Added option to return to the message list after deleting a message in the message view activity. Fixes issue 1634 - fiouzy
* Added preference to go to Integrated Inbox on application startup.  Fixes issue 1797 - cketti
* Implements new setting "Sync remote deletions" which is checked by default.  When unchecked, K-9 Mail will no longer remove messages from local storage just because the message was removed from the server.  This functionality works for all account types.  Messages will still be removed from the local store in order to stay within the limit imposed by the "Number of messages to display" setting.  Fixes Issue 794 - danapple0
* Added the vibrate pattern preference.  Fixes Issue 1915 - jca02266
* Added option to specify the message quote prefix character/string (default ">"). Applied patch provided by fiouzy (Thanks!) with some small modifications.  Fixes issue 1830 - cketti
* Added option to specify Reply-to address for an identity. Patch provided by fiouzy (Thanks!).  Fixes issue 1762 - cketti

## Performance ##

* With all the other performance work we've been doing, getting a message into the messagelist is now much, much faster. Intentionally loading the whole mailbox before we let the user interact with the list is increasingly painful. A 250 message mailbox takes 2+ seconds to "unlock" on a modern phone. - jessev
* Rather than doing a full count of all the unread messages in each and every folder for each message downloaded in a session, factor the "unread messages" count out into the calling code.  It's slightly less clean, but a pretty huge performance gain. - jessev

## User Interface ##

* Remove URLs from message previews. Suggested by Kevin Falcone - jessev
* Show "From", "To", "Cc" headers in "raw" form when displaying all message headers.  Fixes issue 1805 - cketti
* Support inline images in HTML mail. - jca02266
* Don't show "To: " if the only recipients were ccs and bccs. - jessev
* Experimental changes to tell WebView that the HTML versions of messages we generate from ascii are "mobile-optimized" - jessev
* Don't fire off a notification about new mail before we finish initializing an account. This should prevent "(null)" notifications on account setup- jessev
* K-9 wasn't properly clearing the thumbnail/tmpfile cache. We now properly clear it up on start, which causes K-9 to leak less flash memory AND fixes the "Wrong thumbnail" bug. - jessev
* After we download attachments, get rid of the "old" attachment views - jessev
* When marking a message as read as we're opening it from the message list, don't "bother" sorting the messagelist again before opening the message, it adds a _bit_ of slowness when we don't need it and we'll sort again when we get back to the message list. - jessev
* remember which message was selected in MessageList in case the list changes while the dialog is up - thialfihar
* Applied patch by ndarilek (thanks!) to add android:contentDescription attributes to unlabeled input fields to improve accessibility. - cketti
* Moving sendMessage and saveMessage into AsyncTasks to make the send/save buttons much less janky - jessev
* Remember last selected folder for copy and move operations and scroll this folder into view on subsequent folder selections (for move and copy operations). - cketti
* Added account settings for "archive" and "spam" folders. See issue 3. - thialfihar
* Show To, Cc and From headers on the part of additional headers without fetching all headers.  Fixes Issue 1914 - jca02266
* Hide the message header while flipping messages, rather than displaying the "wrong" thing during load. - jessev
* Ask the user to save a draft message or discard it when the press the back button. - jca02266
* Start at replacing text buttons with image buttons. - jessev
* make MessageList remember the last selected item - thialfihar
* Save/restore In-Reply-To/References headers on configuration change Fixes Issue 1887 - fiouzy

## Sync improvements ##

* Android is growing up a bit. artificially limiting the user to attachments of only 5MB is counter-productive, bump the artificial limit to 128M - jessev
* If we hit a race condition and download a message after it's already been marked as suppressed, then don't actually save it locally - jessev
* Only ask for POP3 capabilities before authorization if TLS should be used. Always ask after authorization. - cketti
* A race condition made the outgoing certificate being compared to the old incoming mHost, throwing an exception with an untrusted certificate dialogue to accept or decline. - jessev

## K-9 Mail 2.x Release Notes

## K-9 2.800

## Account List ##

* Integrated Inbox for a unified view of messages across accounts -- danapple
* Flagged message counts are now shown per account and folder -- danapple
* Clickable folder icons always show folder lists, even if there's an auto-open folder -- jessev
* Add confirmation dialogs to destructive account Clear and Recreate operations. -- danapple
* Provide a visually more interesting unread mail counter box that works in both light and dark themes -- danapple
* Display long account names or email addresses a bit better -- cketti
* Provide new facility to totally wipe all data for an account but leave settings intact.  This is useful because sometimes storage is so full that SQLite cannot perform the usual VACUUM or message deletion. -- danapple

## Message List ##

* Optionally only show folders that are subscribed on the server (IMAP only) -- danapple0
* The context menu now has "Search for messages from this sender" -- jessev
* Message preview heuristics have been tweaked a bit -- jessev
* Fade out the bottom edge of message previews in the touch view -- jessev
* Show "(No subject)" for messages without subjects -- cketti
* Move Account and Folder settings into a submenu, along with global settings -- jessev
* When no messages are selected yet, show "select all" instead of the "batch ops" submenu -- jessev

## Message Searches ##

* Keep search results in sync with newly arrived or deleted messages -- danapple

## Message View ##

* Allow users to choose whether they want plaintext messages to be shown in fixed-width fonts -- jessev
* Add option to save and view full message headers -- timmlinder
* Add an account color chip to the left side of the message headers -- danapple
* Show "(No subject)" for messages without subject. -- cketti
* Only show the "Cc" header if there's a Cc. -- jessev
* Always use light theme since we don't change the colors of the WebView. -- cketti
* Always display the date -- cketti

## Composing Messages ##

* Use the default account when composing from search results. -- danapple
* Preserve In-Reply-To and References header when continuing saved drafts -- fiouzy
* Make sure the References header doesn't exceed 997 characters and the value won't get Q-encoded. -- fiouzy & cketti
* Populate text and subject when handling ACTION_SENDTO -- danapple
* Set background color of message compose view to white  -- cketti
* Correctly encode attachment filenames with non-ASCII characters. -- jca02266
* New preference setting to add buttons to attach images or videos which is needed to work around a bug in Gallery. -- cketti (with dman13)

## Settings ##

* You can now set font sizes for account lists, folder lists, message lists and message views -- cketti
* You can now select account chip colors -- jessev
* You can now control account size measurement and search result counting in the Accounts Activity  -- danapple
* You can now optionally make the back button _always_ go up one level -- jessev
* You can now enter alphabetic characters for the Outbox folder name -- danapple
* Add configurable IMAP IDLE refresh frequency in Incoming server settings -- danapple
* Move the configuration for the maximum number of push folders to the Incoming server Settings. -- danapple

## Notifications ##

* Remove notifications for an account when the account's unread message count goes to 0. -- danapple
* The user can elect to have opening a Notification open a account-specific search for unread messages. -- danapple
* You can now select LED notification colors on supported devices -- jessev
* Suppress notifications when new messages arrive in Trash, Sent or Drafts special folders. -- danapple

## Internationalization ##

* Update German translation and adds strings for the new account color picker. -- timmlinder
* Added Polish translation -- borszczuk
* Added Russian translation -- dbriskin
* Removed date_format strings from polish and Russian translation so default values will be used -- cketti
* Removed region code for most translations -- cketti

## Syncing messages ##

* You can now set the maximum age of synchronized messages.  Efficient with IMAP, much less efficient with other Stores.  -- danapple
* K-9 now obeys the system-wide "Auto-sync" setting -- danapple
* K-9 can now sync with Yahoo IMAP servers over Wifi -- danapple
* To help with battery life, give up trying to IMAP IDLE after 10 failures. -- danapple
* To help with battery life, increase IMAP IDLE retry delays -- danapple
* Optionally perform a full poll/sync of a folder whenever the IMAP IDLE connection is established.  -- danapple
* IMAP IDLE can now handle delete notifications from more types of servers (if poll on connect is enabled) -- danapple

## IMAP ##

* Response Parser overhaul -- cketti
* Parser Quoted strings can contain '"' in escaped form. readStringUntil() fails to unescape and will will return prematurely when the string contains an escaped '"' character. -- cketti
* When connected to a server that does not automatically send an untagged UIDNEXT with the responses to SELECT, explicitly SEARCH for the currently highest UID in the folder on the server and then add 1 to it to get the UIDNEXT. -- danapple
* Some servers respond with an untagged EXISTS to every NOOP.  This change parses through all untagged responses in a loop until no more are being generated, avoiding calling internalOpen each time. -- danapple
* Bullet proofing to make fetches not break when folders have insane default visible limits. -- jessev
* Eliminate extraneous spaces from UID SEARCH commands.  Also, abort IDLE connection attempt if mConnection is null before IDLEing.  Otherwise, we cannot set the idle timeout. -- danapple
* Be more tolerant of non-compliant servers.  Do not require a space following the + in command continuation requests.  The rest of the token parser will swallow the space if it is actually there but we don't really need it. -- danapple
* Save IMAP capabilities in upper case so we handle them case-insensitively, too.  Make sure all CAPABILITY-detection constants are written in upper case. --danapple
* After discussion with cketti, reinstate using LIST to find the path delimiter, but only if the server does not support NAMESPACE.  Since we've encountered non-compliant servers that freak out at the LIST command, enclose in a try/catch structure so that failures do not totally prevent IMAP communication. -- danapple
* Correct fault in IDLE WakeLock usage.  The ThreadLocal in MessagingControllerPushReceiver meant that the WakeLock acquired when the DONE was sent was not being released when entering back into IDLE state. -- danapple
* Workaround for servers which don't provide an INTERNALDATE response. -- danapple
* Look for + as acknowledgement that we're in IDLE mode, not "idling" string. -- danapple
* Handle incoming tokens in a case-insensitive manner -- danapple

## SMTP ##

* Prefix IPv6-addresses with "IPV6:" in the SMTP EHLO command -- cketti
* Don't log sensitive information in SMTP commands when "Log sensitive information" is unchecked. -- fiouzy

## WebDAV ##

* Convert to hierarchical traveral from deep traversal. -- mpredosin

## Internals ##

* Eliminate expensive and unused expungedUid map. -- danapple
* Switching "Touch-friendly view" option from message list view no longer causes NullPointerException / Force close -- jessev
* Bump our default target to android-6 since the current SDK doesn't offer an android-5. -- jessev
* Helper classes to get the state of the Android auto-sync setting. Implementation differs for SDK 1.5, SDK 1.6 and SDK 2.0+. See issue 1482 for more details. -- cketti
* Fix error handling code in DB upgrade. -- cketti
* When the remote store is incapable of returning an unread message count (POP) or a flagged message count (POP, WebDAV), count the number of local messages with the desired attributes. -- danapple
* Aggressively get rid of obsolete Flag X_NO_SEEN_INFO.  It was interfering with "unread" searches on POP3 accounts. -- danapple
* Fix NullPointerException when using the touch friendly message list. -- cketti
* Added meta-tag with charset=utf-8 to about box html source to fix UTF-8 issues. -- gerberstef
* Fixed NullPointerException if home button was pressed in AccountSetupBasics activity and no account object was created yet. -- cketti
* Use android.text.util.Regex.EMAIL_ADDRESS_PATTERN for email address verification before account creation. -- cketti
* Handle accounts with negative (invalid) saved display count values. -- danapple

## K-9 2.600

* Auto-complete for contacts from all address books (Android 2.0+) -cketti
* Added support for sharing multiple files (Android 1.6+) -cketti
* Folders that are marked as belonging to the "top group" are shown first in the folder list -danapple0
* Display parts of the mail while rest is being downloaded -baolongnt
* Add support for most batch ops (except move and copy) in search results.  Add support for batch move and copy on real folders.  -danapple0, cketti
* Provide title bar display of next polling time -danapple0
* Provides for menu items and hotkeys to quickly change the mode of displayed folders -danapple0
* Add 's' hotkey for Select/deselect -danapple0
* Allow user to turn off gesture-based control -danapple0
* Implement DEFLATE compression for IMAP communication -danapple0
* Use 8bit transfer encoding for sending mails if available, otherwise quoted-printable -cketti
* Use real hostname in SMTP EHLO command or fall back to IP (rather than localhost.localdomain) -cketti
* Don't send simple text mails as multipart messages -cketti
* Unified location of "Check mail" button in app menus -danapple0
* Better new-mail notification system. -danapple0, skister2
* Stop views from "resetting" to previous intents on rotate -jessev
* Make use of email signatures optional -jiri.tyr, cketti
* Sent folder can be set to "-NONE-" to prevent emails being saved -danapple0
* Security: Don't load any external resources (iframes, linked stylesheets) unless "show images" is pressed -cketti
* Improved IMAP push -danapple0
* Only reschedule polling and setup pushing when necessary due to particular setting changes.  Makes the K-9 Mail UI much more responsive to setting changes that do not affect polling and pushing -danapple0
* Advanced poll scheduler. Works more reliably -danapple0
* Several fixes in the implementation of the IMAP protocol -danapple0, cketti
* Fixed issue with passwords containing special characters, e.g. ":" -cketti
* Fixed issue where the background turned black when scrolling through the message list on some devices -cketti
* POP3: Tolerate malformed UIDL response -danapple0, bengnc
* Bullet-proofing for database upgrades and better fallback for failures -jessev
* Updated german translation -wertarbyte, henning.stummer

## K-9 2.403

* Checkbox rendering fixes on Android 1.5
* Resolve an issue that caused search result action force-quits

## K-9 2.401

* Bugfixes for 2.0 -> 2.4 upgraders

## K-9 2.400

## Significant new features ##

* It's much faster!
* Implement local message search - jessev
* Guess mime type (when not specified) of attachments of received messages using file name extension so that we can open them - baolongnt
* Headers in Accounts, Folder List and Message List now show unread count and background processing activity -- danapple0

## Performance ##

* lazify loading of message headers in LocalFolder, since they're not needed in most of our use cases. -jessev
* When we load up the "Accounts" activity as K-9 starts up, jump straight to either the FolderList or the MessageList rather than always going through the FolderList - jessev
* Kill the no-op Welcome activity to improve startup performance - jessev

## Message List ##

* Added a new "touch friendly" style with message previews - jessev
* Made it possible to enable or disable "stars" for flagged messages - jessev
* Added swipe-to-select for operations on multiple messages - jessev
* Eliminated old multi-modal message lists. - jessev
* There is now an Expunge action in the option menu. - danapple0
* A new "Batch ops" option menu in Message List.  Provides flag/unflag, mark as read/unread and delete and select/deselect all.  Move and copy are partially implemented, but disabled. -danapple0
* The "Sort by..." menu now toggles ascending/descending when the currently selected sort mode is clicked. -danapple0
* Incorporate Vincent Lum's new icon for the batch ops sub-menu -danapple0

## Message Composition ##

* Eliminate carriage returns from reply and forward text.  (Fixes Issue 518) - danapple0

## Folder List ##

* There is now an Expunge action in the context menu (long-press on the folder). - danapple0

## Configuration ##

* Add a global preference for enabling animations, beyond those that are necessary.  Defaults to "enabled." -danapple0
* 250, 500 and 1000 messages may now be synced per folder. - jessev
* Allow user to set a limit on the number of folders to be handled with push technology. - danapple0

## Protocol Support ##

* Initial implementation of CRAM-MD5 support for IMAP and SMTP. (Patch contributed by Russ Weeks <rweeks@gmail.com> ) - jessev
* Only use the IDLE timeout for the folder while actually in IDLE state.  (May help with Issue 1022, Issue 952, Issue 895) - danapple0
* For IMAP accounts, it is now possible to disable the copying of deleted messages to the Trash folder, by setting the Trash folder to -NONE-. - danapple0
* Each IMAP account can be set to expunge messages in a folder as soon as a move or delete is performed on the folder ("immediately"), each time the folder is polled, or only when executed manually. - danapple0
* For WebDAV accounts, the user can now choose the server-side equivalents of the special folders, just like for IMAP. - danapple0
* Improvements to cut down on the number of IMAP server connections opened - -danapple0

## Platform Integration ##

* Implemented delete intent broadcast using a modified patch from stephane.lajeunesse - baolongnt
* Delete intents are now broadcasted when we delete a message because it was deleted on the server - baolongnt
* Implementation of a Receiver and Service to provide for the capability to accept control from other Android applications.  Allows for changing both Account-level and global settings.  Account-level settings can be applied to a single Account or to all Accounts. - danapple0

## Internals ##

* Code namespace changed back to com.fsck.k9 (as it was before 0.31) - jessev
* Basic code style cleanups suggested by IntelliJ - jessev
* Let IntelliJ optimize our import statements - jessev
* Replace MessageList, FolderList and AccountList's outdated message-passing mechanism with runOnUiThread for improved code clarity and performance. -jessev
* Killed a bunch of debug logging that clutters (and slows) production apps - jessev
* Make it possible to instantiate a localFolder by id. - jessev
* Incorrect (and ancient code) would accidentally clobber the wrong attachment (and only one, rather than all) when deleting a message. - jessev

## UNRELEASED - K-9 2.200 (As of 2.108 / r1109)

## User-visible features ##

* Overhaul our setup wizard to have a more reasonable bottom bar and to reuse that layout code where possible; standardize the id of the 'next' button - jessev
* "Starred" messages in MessageList and Message views - jessev
* Bulk-flag, delete and "mark as read" for messages - baolongnt,danapple0,jessev
* Vincent Lum's new icon! - jessev
* Added option to choose which side the widgets in message list will be on - baolongnt/jessev
* when we go into a folder for the first time, jump to the first message being displayed rather than the widget at the end of the list - jessev
* Implement References/In-Reply-To/X-User-Agen. Fixes issue 42. Patch from e.w.stemle - jessev
* You can now "swipe" left or right in the Message view to go to the previous or next message, respectively - jessev
* Slight cleanups to various messages - jessev
* Fix our user-agent to be K-9 Mail - jessev
* First pass at stopping the "Sending messages" notification when there's nothing to send. A better fix would take into account not sending flagged messages - jessev
* fix the header background color to not ignore theme in horizontal mode - jessev
* When flipping messages, be sure we jump to the top as we display - jessev
* Add double-tap at top or bottom of a message to jump to the top or bottom of the message - jessev
* Improvements to render quality of plaintext messages. - jessev
* Added a message-flip animation. Long-term, we want a ViewSwitcher instead - jessev
* New sort-by and reverse-sort icons by Vincent Lum - jessev
* Deleting messages in messageView now preserves the direction the user was "travelling" in before the delete - jessev
* Provide additional date format display options in Preferences - danapple0

## IMAP ##

* Major IMAP IDLE rework: 1) Actual message sync on MessagingController uses same connection as    IDLE for faster and more efficient push operation.  Uses fewer    connections to the server. - danapple0
* Fix notifications for messages that arrive via IDLE - danapple0
* Eliminate race condition which causes multiple connections to IDLE simultaneously - danapple0
* Bulk deletion for IMAP. - danapple0

## WebDAV ##

* Bulk delete for WebDAV - danapple0

## POP3 ##

* The closest thing to bulk delete for POP3.  Stupid POP3 has no multiple delete command, but at least we can index UIDs in one shot. - danapple0

## Internals ##

* Implement Bao-Long's suggestion to modify LocalStore.purgeToVisibleLimit only to work on undeleted messages. - danapple0
* Fixed error handling code when trying to view a message that does not exists This code was lost during the merge of issue4-1.x branch - baolongnt
* Made K-9's code consistent with astyle - jessev
* Remove 100loc of  custom date formatting code which worked around a deficiency in the android 1.0 API - jessev
* Implemented delete intent broadcast using a modified patch from stephane.lajeunesse (Issue 810) - baolongnt

## Performance ##

* First pass at doing our "don't show deleted messages in lists" filtering in SQL - Jesse Vincent
* Switch to UUIDs for message-id generation. They're just as unique and lighter weight to calculate - jessev
* Defer creation of a MessageId until we actually need it. This was ~75% of our message load-from-db time - jessev
* Make item-clicks in MessageList a little less convoluted - jessev
* LocalMessage no longer bothers to populate the headers of the MimeMessage backing it until writeTo is called - jessev
* Added a setMessageId method to MimeMessage rather than just explicitly setting a header - jessev
* Cache the message subject in a localMessage, rather than roundtripping through a MIME message - jessev
* Now that our not-deleted-messages mode works, actually use it in messagelists - jessev
* Add "deleted" to our default database indexes. (wants some query analysis at the SQLite level) - jessev

## Build ##

* Get K-9 building on Java 1.5 again - jessev
* Make build-beta name beta builds more correctly - jessev
* give myself a release-to-gcode target - jessev
* Make release builds include the version number from the manifest - jessev
* hardcode fewer things in tools/build-beta, don't blow away our attachment-saving permission - jessev
* add a beta app name string, so we don't need to hardcode it in tools/build-beta - jessev
* add an automated zipalign step to our build process - jessev
* Updated ignore pattern to exclude gen and bin fodler - baolongnt
* Building in eclipse corrected a bug in default.properties - jessev

## Issues fixed ##

* Issue 860 - danapple0
* Issue 856 - danapple0
* Issue 844 - danapple0
* Issue 870 - danapple0
* Issue 857 - danapple0
* Issue 850 - danapple0
* Issue 847 - danapple0
* Issue 835 - danapple0
* Issue 827 - danapple0
* Issue 734 - danapple0
* Issue 784 - danapple0
* Issue 657 - danapple0
* Issue 801 - danapple0
* Issue 188 - danapple0
* Issue 394 - danapple0
* Issue 763 - danapple0
* Issue 673 - danapple0

## K-9 2.0 (Android 1.5, 1.6, 2.0)

* 2.000
  * *Push mail for IMAP accounts* using IMAP IDLE - danapple0
  * Accelerated message list loading using pipelined architecture - danapple0
  * More efficient background queue processing - danapple0
  * Improved unread-count tracking - danapple0
  * Improved threading model in MessagingController for higher efficiency and better responsiveness - danapple0
  * Disable polling and pushing when no network is available - danapple0
  * Provide explicit menu items to send messages from the Outbox - danapple0
  * Restore operation of hot keys and zoom when viewing a message - danapple0
  * Restore detection of hard keyboard to control display of navigation buttons when viewing a message - danapple0
  * Persist a copy of the message list when rotating for faster response - danapple0
  * Better deletion of IMAP messages and logging when deletion failures occur - danapple0
  * WebDAV / MS Exchange improvements: move, copy, delete and mark as unread now work. - danapple0
  * Fixes for WebDAV connection problems. - danapple0
  * Fixed bug where wrong messages is displayed after screen rotation (issue 556) - baolongnt
  * Respect global 'Background data' preference and provide ability to turn of all synchronization. - danapple0
  * Fixes for running on Android 2.0 - danapple0, jessev
  * SMTP and IMAP protocol fixes - danapple0
  * "Delete from server" is now the default for IMAP andxzz WebDAV accounts - danapple0
  * New sort-by and reverse-sort icons by Vincent Lum
  * Made the date column a bit wider to fully view time timestamp and view more of the date in landscape mode - baolongnt
  * We now accept all mime types when handling the android.intent.action.SEND intent - baolongnt
  * Normalize padding in setup widgets; turn off "fading edges" in setup widgets - jessev
  * Added content-type detection using file name extension (This adds better integration with OI File Manager which implements the ACTION_GET_CONTENT intent) - baolongnt
  * Remove duplicated "make this account the default" option during setup - jessev
  * Give the user the choice of whether to enable Push as they create a new IMAP account. - jessev
  * Make the status messages for folders that are being synced with push a bit prettier - jessev
  * Don't show folder choosers before we have a working account -jessev
  * Reorganize the Account Settings page, regrouping options by "what a reasonable user might want to change at the same time" - jessev
  * Make our use of email/mail consistent. It's "mail" except for "Email address" - jessev
  * Rephrase a number of unclear or oddly phrased messages (many dating from AOSP) - jessev
  * Slightly better MessageView fit-to-screen and scrollbar flow - jessev
  * First pass attempt at fixing the "Android 2.0 breaks contacts lookup" issue
  * K-9 now targets Android 1.6 (with 1.5 support) to support QVGA devices - jessev
  * K-9 now  provides 2 - 24 poll check periods - danapple0
  * Account setup UI overhaul - jessev
  * Record permanent failures into K9mail-errors -danapple0
  * K-9 now detects "Aw:" as being the same as "Re:" - jessev
  * Enable fast scroll in message view, list view and "pick a folder" dialogs -jessev
  * Sort the "NONE" folder to the top of the "auto-expand folder" list -jessev
  * All our layouts now use dip instead of px, for great weird-resolution weird-screen-size justice - jessev
  * Better italian translation - tauromenion
  * Fixed issue with reply icon in subject in message view screen not being set properly when navigating around using up and down arrows - baolongnt
  * Displays a warning and allows K-9 to continue in some situations that otherwise cause a crash.  -danapple0
  * Eliminate secret debug preferences activity.  Add debug preferences to global preferences. - danapple0
  * WebDAV: Automatically add / separators if not supplied by user.  -danapple0
  * WebDAV: Set authentication header for downloading and sending messages, so that those functions work with sites using Basic authentication. -danapple0
  * WebDAV: Don't swallow log Exceptions.  Instead, allow Exceptions to percolate up to higher levels so that they can be logged into K9mail-errors. - danapple0
  * WebDAV: Provide appendMessages function, so that Drafts get stored on the server - danapple0
  * Prevent a crash and put up a Toast when invalid data is entered early in the new-account setup workflow - danapple0
  * MessageListener.synchronizeMailboxNewMessage() does not requires a context to be passed anymore. -baolongnt
  * We use the application as context when broadcasting message received intents - baolongnt
  * Refactored intent constant classes - baolongnt
  * Better icon for the About menu item - baolongnt
  * K-9 now broadcasts intents for new emails: com.android.email.intent.action.EMAIL_RECEIVED (issue 607) - baolongnt
  * Added intent for viewing individial messages - baolongnt
  * Issues fixed in since 1.0x:
    * Issue 4 - danapple0
    * Issue 44 - baolongnt
    * Issue 59 - danapple0
    * Issue 133 - danapple0
    * Issue 290 - danapple0
    * Issue 448 - danapple0
    * Issue 482 - danapple0
    * Issue 538 - danapple0
    * Issue 551 - danapple0
    * Issue 556 - baolongnt
    * Issue 573 - mpredosin
    * Issue 574 - danapple0
    * Issue 577 - danapple0
    * Issue 587 - danapple0
    * Issue 587 - danapple0
    * Issue 589 - baolongnt
    * Issue 589 - baolongnt
    * Issue 606 - mark.himsley
    * Issue 628 - danapple0
    * Issue 650 - danapple0
    * Issue 651 - danapple0
    * Issue 654 - danapple0
    * Issue 656 - danapple0
    * Issue 657 - danapple0
    * Issue 668 - danapple0
    * Issue 673 - danapple0
    * Issue 675 - danapple0
    * Issue 682 - danapple0
    * Issue 696 - danapple0
    * Issue 708 - themaninthesuitcase (updated by danapple0)
    * Issue 710 - baolongnt
    * Issue 712 - danapple0
    * Issue 722 - danapple0
    * Issue 733 - danapple0
    * Issue 737 - danapple0
    * Issue 738 - danapple0
    * Issue 742 - danapple0
    * Issue 742 - danapple0
    * Issue 744 - jesse
    * Issue 747 - danapple0
    * Issue 752 - danapple0
    * Issue 753 - baolongnt
    * Issue 761 - baolongnt
    * Issue 762 - baolongnt

## K-9 Mail 1.x Release Notes

## K-9 1.x Release Notes (Cupcake and Donut only) #

* 1.011 (r843) October 19, 2009
  * Switch from left right arrows to down up ones - baolongnt
  * Eliminate duplication of header fields on rotation - baolongnt
  * Prevent merging of reply drafts - baolongnt
  * Delete temporary drafts when sending - baolongnt
* 1.010 (r828) October 13, 2009
  * Thread safety for MessageView - baolongnt
  * Fixed duplicate Message-IDs - baolongnt
* 1.009 (r813) October 3, 2009
  * Performance improvements - baolongnt
  * Thread safety for MessageList and FolderList- baolongnt & danapple0
* 1.008 (r785) September 26, 2009
  * Work around Yahoo (and possibly other IMAP servers) faults with deletion, flagging, etc. - danapple0
  * Fixed missing Date header (issue 626 and issue 528) - baolongnt
  * Fixed issue 639 and issue 646 preventing from sending emails when the self-Bcc setting is empty - baolongnt
* 1.007 (r766) September 22, 2009
  * Reduced APK size back to 6xxK (r753)- baolongnt
  * Updated Italian transalation. Thanks tauromenion - baolongnt
  * Fixed issue 560: Auto-expand set to NONE creates folder called NONE - baolongnt
  * Fixed issue with encoding of email addresses in the header with non ascii characters (issue 598) - baolongnt
  * Fixed issue 261: Bad parsing of email with commas in their display names - baolongnt
  * Fixed issue 541: Switched all international versions to use < and > characters instead of text - baolongnt
* 1.006 (r728) September 4, 2009
  * Prevent crashes if user attempts to sort while folder is loading. - danapple0
  * Prevent crashes when searching for a suitable intent to dispatch to fails - danapple0
  * K-9 now uses the correct identity on reply-to (Issue 530) - danapple0
  * Identity Pages now respect themes (Issue 505 - danapple0
  * Improvements / fixes for "Load more..." behavior (Issue 575) - danapple0 and mpredosin
  * Restore missing "confirm" dialog on "mark all as read" (Issue 503) - danapple0
  * Fix for a SQLite placeholder overflow (Issue 553) - danapple0
* 1.005 (r712) July 1st 2009
  * Fixed numerous issues (249, 524 and more) when loading message for view - baolongnt
  * Many UI and navigation fixes - baolongnt
  * Fix UTF8 headers (such as subject) - typester
* 1.003 (r700) June 24th 2009
  * Fixed parsing of Cc and Reply-To headers - baolongnt
* 1.002 (r685) June 14th 2009
  * Choosing -NONE- as default folder makes the folder list open when an account is opened instead of the -NONE- folder - baolongnt
  * Added 'add to contact' feature when one clicks on the from address in message view (code taken from AOSP) - baolongnt
  * Display Cc recipients - baolongnt
  * Display both date and time on old messages - baolongnt
  * Support for multiple identities (address/name/signature) for sending email. - danapple0
* 1.001 (r665) - June 1 2009
  * First BETA release of the K-9 1.x series - expect instability (but report bugs)
  * Major rework of folder and message lists. (They're now separate)  -jessev
  * Significant performance improvements to message and folder lists -jessev
  * User-selectable theme -jessev
  * Usability/UI Improvements to new-account workflow - jessev
  * Fix for hand-entered email recipients containing non-ascii characters - ludovic.lange.android

## K-9 Mail 0.x Release Notes

## K-9 0.x Release Notes

* 0.114 (r631) - May 22nd 2009
  * Fixed NullPointerException when trying to send message with to text body (attachment only for example) - baolongnt
  * When no accounts are set for automatic sync, the MailService is stopped - danapple0
  * New spanish translation from Ivan Jimenez <ijs1980@yahoo.es>
  * Exchange updates to fix authentication issues - based on design and counselling from ismarc31 - jessev
  * Added option to have signature before quoted text - baolongnt
  * Proper display of plain text message indentation - baolongnt
  * Fixed issue 423: Sending to Thunderbird client: double message appearance - baolongnt
  * Fixed issue 404: App crashes when trying to download large plain text messages - baolongnt
    * IMAP only: Only messages under 50K are downloaded during background sync. For larger messages, only the header is downloaded. The body is downloaded when the message is opened
  * POP3: Large messages are only downloaded once (vs both during background sync and upong opening the message) - baolongnt
* 0.113
  * Fix for deletion of messages in POP3 account - danapple0
* 0.112 (r495) - May 5, 2009
  * expose download status of messages. slightly grey out messages that haven't yet been downloaded fully  -  jessev
* Flip the message color background to white in the message list for better contrast - jessev
* Make sure not to lose an untagged EXISTS, even if it came during an IMAP command that we didn't handle properly. - danapple0
* Proposed fix for "unreopenable" folders bug - jessev
* 0.111 - May 4, 2009
  * MessageView URL opening bug fixes - jessev
  * mail reply fixes - jessev
  * Fixed "auto-open" for INBOX - danapple0 (code by jessev)
  * IMAP Protocol reliability fixes - danapple0
* 0.110 - May 3, 2009
  * *This release has a database change. K-9 will automatically redownload your synced email*
  * Message View and Message List performance improvements by debachedsloth++
  * Performance improvements to message display - jessev
  * Improved background synchronization reliability - danapple0
* 0.109 - April 29, 2009
  * Market release
  * Ringtone preferences should now be saved properly (danapple0)
* 0.108
  * Market release
* 0.107
  * Improved IMAP handling - danapple0
  * New, "light" visual theme and other UI cleanups -jessev
* 0.104
  * New preferences storage mechanism to eliminate lost accounts - danapple0
  * Improved SMTP and POP3 handling - danapple0
  * Preference for controlling which folder, if any, is automatically opened - danapple0
* 0.103
  * Add ability to manually check for new mail on all accounts, specific accounts, and specific folders - danapple0
* 0.102
  * Messages can be moved and copied between folders - danapple0
  * All sent messages are shown with recipient email address instead of sender. - danapple0
* 0.100
  * Changed threading to Sorting so there is now sort by date, subject, sender, flag, unread, and attachments.  Also added ability to reverse the sort order. - danapple0
  * Fixed possible index issue with less emails in the inbox than the requested display message size for WebDav - ismarc31
  * Provide proper relative URL support for authentication, providing support for Exchange 2007. - ismarc31
* 0.99 (r352)
  * Folder threading (toggle with T hotkey or option menu) - danapple0
  * Fix for regression on k9mail software update on WebDav(Exchange) accounts - ismarc31
  * Per account notification - baolongnt
  * Display the number of new messages on the email notification icon - baolongnt
* 0.97 & 0.98
  * Improvements to "instant" delete functionality in folder/message list and message view - danapple0
  * Authentication method for WebDav(Exchange) changed.  Support for non-Form Based Authentication. - ismarc31
* 0.96
  * "Instant" Empty Trash - danapple0
  * Configurable navigation buttons for viewing messages - danapple0
  * Option to add path for login page off of base URL for WebDav(Exchange) - ismarc31
  * Option to add path off of base server to post Form Based Authentication credentials to. - ismarc31
  * Option to add mailbox path off of base server for authentication and data requests. - ismarc31
  * New data model for returned XML data from WebDav requests - ismarc31
  * Handle uppercase MIME types from non-compliant MUAs - gwillen & Josh Guilfoyle
  * Add ability to suppress ongoing synchronization notifications - debauchedsloth
* 0.95 2009-01-17
  * Add ability to "flag" messages - danapple0
  * Indicate which message have been replied to - danapple0
  * Fix links in plain text messages - danapple0
  * Improved synchronization timing - danapple0
  * Added support for pulling login URL for WebDAV login - ismarc31
  * Added default URL for mailbox path if path isn't returned after WebDAV login - ismarc31
* 0.94
  * Added help messages in Message and Folder List views, shown when the 'h' key is pressed
  * "Secret" debug screen now shows version number. (As opposed to version name, shown in the about dialog, and SVN revision, which is what was formerly and often inaccurately shown there.)
  * Faster ("instant") message deletion - danapple0
  * All message operations can be done on messages in the Outbox - danapple0
  * Deleted messages in Trash now have full body - danapple0
  * Deleting a message in Trash deletes it permanently - danapple0
* 0.93
  * Fixed send issue in WebDAV-- messages not being sent.
* 0.92
  * DAmail merged into K-9 (Daniel I. Applebaum)
    * Ability to sync multiple folders
    * True sync-while-sleeping
    * Better sync indicator
    * Ability to show only certain folders
    * Mark all as read
    * Performance improvements
    * Empty trash
    * Show unread message counts
    * Delete policy is more flexible
  * WebDAV (Exchange) Fixes
    * Fix for some instances of subfolders not working.
    * Fixed an issue where some Exchange servers wouldn't mark messages as read.
    * Fixed incorrect URL encoding in some folder path names.
    * Fix for date/time issues prior to fetch(BODY) or fetch(BODY_SANE) is called.
    * Added support for proper deletions in WebDAV. Deleting a message the first time moves it to the (currently hardcoded) Deleted Items. Deleting an item in 'Deleted Items' will remove it completely.
    * Added self-signed certificate/invalid certificate hostname support
* 0.91
  * Unreleased
* 0.90
  * Unreleased
* 0.31
  * code renamed to ease merging - jessev
  * Added silent 'ongoing' notification when checking email - baolongnt
  * New email notification now includes purple blinking LED - baolongnt
  * More reliable background sync by using wake lock --> Background sync now work even when the device is asleep (Thanks danapple0) - baolongnt
  * Added about dialog. - young.bradley
  * Update the domain validator to accept ip addresses and "localhost" - young.bradley
  * block server entries in AccountSetupIncoming.java and AccountSetupOutgoing.java that are not valid domain names. - young.bradley
  * Update for folders to display the "path" that the folder resides in (ie, Inbox/Work/To Me) rather than just the name of the folder.  Allows an easier understanding of the hierarchy until true folder trees can be implemented. - ismarc31
* 0.22
  * EXCHANGE Fixed an authentication issue with WebDav support that caused by the last update. -ismarc31
  * Added support for scrolling on 2 of the screens in the account setup process to allow the full steps to be done in landscape mode.  There are some graphical regressions in portrait mode from it that aren't resolved yet. -ismarc31
  * EXCHANGE Removed display of path prefix.  System now pulls all folders/subfolders.  Does not display in a hierarchy, but will load messages from each folder.  Only the final folder name is displayed instead of the path off of Inbox right now - ismarc31
  * EXCHANGE Added change to populate the url for the mailbox each time authentication occurs (url is in the response).  Updated initial message population to use reverse order so initial display has newest first (only noticeable on slow connections). -ismarc31
  * Added the following 2 keyboard shortcuts in message list view:  -baolongnt
    * S: Setttings
    * L: Last auto sync date time
  * Fixed bug # 122: Some keyboard shortcuts only work when a message is selected - baolongnt
  * fixed bug # 87: Acccounts all refresh at the same freqency - baolongnt
  * Fixed bug # 82: Messages lose their read status when getting new emails from POP3 server - baolongnt
  * FIxed bug # 81: Notifications never show up wen using POP3. - baolongnt
  * Added back auto-start of background message sync (most likely removed during merge) baolongnt
* 0.21
  * Initial implementation of Exchange support - ismaarc
  * Ability to do new-mail notifications without ringtone - jesse
  * Rejiggered message list views
  * Reordered buttons in message create/display
  * Configurable number of messages to fetch / display - ismarc
  * Custom Sent / Deleted folders - bradleyy
  * IMAP Prefix fixes - brock.tice
  * Self-signed certificate fixes - bradleyy
* 0.18 - 2 December 2008 - Market Release
  * Various IMAP fixes - Brock Tice
  * Support for self-signed SSL certificates - Bradley Young
  * UI tweaking and compaction - Jesse Vincent
  * Added 'd' keybinding in message list. - Jesse Vincent
* 0.14 - 2 November - 2008 - Market Release
  * Key binding - d for delete in message view - jesse
  * Message display performance improvement - jesse
* 0.13 - 2 November 2008 - Market Release
  * Bug fixes for account creation - tibbetts
  * Typo fixes - tibbetts
* 0.12 - 1 November 2008 - Market Release
  * Further message list performance improvements. - jesse
* 0.10 - 1 November 2008
  * Fixes for users without email signatures - jesse
  * Major (I hope) performance improvements in message list - jesse
* 0.9 - 30 October 2008
  * Replaced handcoded linkifier code with android.text.util.Linkify for more consistent linkification
* 0.8 - 30 October 2008
  * email signatures -jesse
  * message composition settings page - jesse
  * message composition button cleanup - brock
  * removing a weirdly placed "add another account" from within an existing account's preferences - jesse
  * icon restretching - sparrowhawk
* 0.7 - 29 October 2008
  * Allow users to change the From address associated with their accounts
* 0.6 - 29 October 2008
  * Identify ourselves as localhost.localdomain instead of localhost to appease fascist SMTP servers - jessev
* 0.5 - 29 October 2008
  * Patch for generating correct "empty" MIME messages - brock.tice
  * Support for tabs in IMAP responses for compatibility with Mac OS X Server - yostinso
  * "Bcc myself" functionality - jessev
* 0.4
  * Fixed hardware "back" button support in message view
* 0.3
  * Added keybindings in message view
* 0.2
  * Added keybindings for reply/reply all/forward/etc in index view
* 0.1
  * Initial release.
  * Support for the Delete key in message lists
  * Deleted message status pushes back to your IMAP server

