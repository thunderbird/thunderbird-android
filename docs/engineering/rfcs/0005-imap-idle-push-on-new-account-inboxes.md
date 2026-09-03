# RFC 0005: Enable IMAP IDLE ("Push") for the Inbox by Default on Newly Added IMAP Accounts

- Issue: [#11320](https://github.com/thunderbird/thunderbird-android/issues/11320)
- Status: **Accepted**

## Summary

When a user adds a new account, if it is an IMAP account and IMAP IDLE is supported on the server, we will turn on
"push" syncing for the inbox folder using IMAP IDLE. If they choose to enable notifications, they'll receive
notifications when they receive new emails in as close to instant as we can get it without a true push service.

## Motivation

Users complain that their emails do not show up on time. When they learn they can already activate IMAP IDLE, which we
call "push" to the end user, they find that the setting is buried in an unexpected place. They may also misconfigure it,
causing their email to stop working altogether. While IMAP IDLE works on a per-folder basis, the end user doesn't want
to have to think about this level of detail. They don't want to have to figure out server capacities for open sockets
and pick their most important folders. They just want their new mail when it comes in. Therefore, we'll activate
our existing IMAP IDLE implementation for their Inbox, what they'll see as "push," which will give them a notification
or instant syncing of their inbox when changes appear in that folder on the server. Most users will never feel the
need to change this setting, and will believe it's working as it was always intended.

## Proposal

We will enable our existing push service for IMAP accounts during account creation for new accounts or those imported
via a QR code. We will not change the settings for users who are importing from a file, as we can't tell if they chose
to disable push previously. If a user is not importing folder settings, and they're adding a new account, we will enable
our existing IMAP IDLE sync, "Push" on their Inbox folder, which we'll identify during setup.

#### Method

Whether or not a folder has push enabled is a boolean stored locally in `FolderDetails.isPushEnabled`, with those
details saved to the device's storage in `FolderSettingsDataStore`. With this proposed change, we will update the
folder's details to contain `isPushEnabled = true` when creating a folder details instance for the account's inbox
folder.
This alone will not enable IMAP IDLE folder syncing though. Without `canScheduleExactAlarms()` returning true,
we can set up an IMAP IDLE socket, but it cannot refresh at least every 29 minutes, as is required for the
[IMAP IDLE spec](https://datatracker.ietf.org/doc/html/rfc2177). We will need to ensure that we have some form of
fallback here, including fetch, for accounts that have enabled "push" IMAP IDLE syncing without the necessary
permissions to keep the socket open. Fetching can serve as a fallback, if we have the necessary permissions for that
as well.

The exact process to create a new `FolderDetails` instance when generating a new account, and not any time an instance
is created, would require further investigation. It will likely involve getting server capabilities in the
`AccountCreator.create` function, checking for the `Capabilities.IDLE` server setting enabled in the
`RealImapConnection.isIdleCapable` property, and then getting the specific Inbox folder (`LegacyAccount.inboxFolderId`)
and enabling the `isPushEnabled` variable. However, we will still have to determine where is the most efficient way to
do this in that it is set up during account creation, before the user's inbox has loaded, and does not require a great
deal of dependency changes to make the information available.

We may also want to ensure we do this after getting all necessary permissions to keep an IMAP IDLE socket open, or
simply ensure we have a good enough fetch fallback available so that push syncing will be enabled after a user has
granted the permission. This will require further technical investigation, but these are
potentially issues that exist today in our current implementation.

## Alternatives Considered

Initially, we had hoped to enable IMAP IDLE on **all** folders that could support it. However, there are limits to the
number of folders that can be synced through IDLE sockets with IMAP. The limit is server-specific, and some may fail
silently. Others present an error that reports they have too many connections. An end user who isn't aware of the
limitations of IMAP IDLE would not know what this means, and would therefore not be able to solve the issue without our
help. The end result would be issues for most—if not all—users who set up an IMAP account using Thunderbird or K-9 that
would completely disable their account until we can walk them through the many steps to fix it.

We _could_ solve this by including some information in the Folder Settings view on the limits of folder syncing, but the
goal here is to reduce the need of users to change these settings at all.

By defaulting push notifications on for newly added accounts, users won't need to set up individual folders to sync
because they'll
get their messages when expected. If they have a more complex setup or want to increase the sync time of items in other
folders, they can still do so manually. When we make this process easier later this year, we'll want to notify the user
that activating push on too many folders may break their connection to the server. Changing these user-facing settings
menus is out of scope for this specific project, but will be part of future notification settings improvements. We may
potentially perform a fetch on all folders when we notice a change on the inbox, which would have the same effect as
syncing with all folders as the message would likely first appear in their inbox before sorting anyway, but this may be
an option we want to present to the user, as it could cause battery drain and use larger amounts of data than IMAP IDLE
would alone.

## Risks & Drawbacks

IMAP IDLE keeps a socket open to receive information from the server when there are changes to the folders they're
watching. This will be the Inbox folder. If a user receives many email messages throughout the day, it could potentially
lead to more fetches, and more battery usage. In my personal testing, there seemed to be no difference in battery
consumption between fetching all folders every 15 minutes and turning on IMAP IDLE for multiple accounts, and push on
for multiple folders as well, more than our end users would get with the default settings proposed here. Users can turn
the feature off if it becomes an issue, but IMAP IDLE was made specifically to reduce the need to sync with a server
frequently and reduce both battery usage and network bandwidth. As a result, it may reduce battery consumption for
our users. IMAP IDLE can reduce periodic polling, but its net battery impact depends on connection stability,
network conditions, server behavior, account count, and incoming-mail volume. Those who see an increase would
likely have an account where they receive many emails, but not all at once, such as automated emails every few  
minutes. Someone with such a setup could turn off the setting where it exists today. In a future project, we'll
make these settings even easier to access.

In Google Play, we need to make a declaration for Foreground Services (FGS) we use. "Push" uses a `specialUse` FGS.
We define this in the manifest under the name `com.fsck.k9.controller.push.PushService`, a specific type of
`dataSync|specialUse`. The property we set up states:

> "This service is used to maintain a continuous connection to an IMAP server to be able to provide instant notifications
>
>> to the user when a new email arrives. Firebase Cloud Messaging is not suitable for this task, neither are mechanisms
>> like AndroidX WorkManager. Other foreground service types aren't a good fit for this use case."

This remains true, but Google may ask us to update the statement pointing out that the existing service will be
enabled by default if a user is setting up an account that can use IMAP IDLE syncing and has given us the necessary
permissions to do so. We will also possibly have to update our documentation with the flow for turning this off, as
it's enabled by default and the user will not have interacted with the setting to enable it previously. Finally,
we'll have to update the video we provide to Google Play showing how this service is deactivated, should a user
choose to do so. Without taking these steps, we could face a risk of a rejection from the Google Play Store, though
we will wait to see what Google has to say about the change. Because IMAP IDLE, "Push," is an existing feature, already
cleared for a special use case, we may not need to make any changes here.

## Open Questions

It's rather unfortunate that there's no way to be sure of how many sockets we can keep open for each server. Some are as
little as 2-4, while others can support IMAP IDLE for 8 folders. There's no way to test this besides creating a failure
state. We could do so, keep a list of the maximum number of connections and, if a user has fewer folders than that,
enable IMAP IDLE syncing on each of their folders. However, a vast majority of our users will only need this on the
inbox and perhaps, if they do server-side filtering, an "important" or "flagged" folder. But by doing a full fetch on
each change on the Inbox, we may actually resolve this, as the emails will likely end up in the inbox first, before
sorting, and then the filtering or automation they're doing could place the email elsewhere. Either way, we'd trigger a
sync because we're waiting for updates to the inbox. We may want to revisit this idea later, but, for now, simply
activating IMAP IDLE on the Inbox folder may be sufficient to solve user complaints.

#### Future Considerations

We call this "push," which is most closely related to notifications and push notifications go through a server we
wouldn't own and therefore wouldn't be private. But these are not push notifications. We instead are using IMAP IDLE
to keep a socket open, then notifying the user locally when the server is updated. As a result, we're not using a
push service, and, if the user has notifications disabled, we could still be doing the (near) real-time syncing.

We may want to consider documenting this feature better, and removing the "push" toggle, instead using an "instant
syncing" toggle that, when turned off, will allow the user to select a fetch interval. This is a more accurate
representation of what "push" is, a replacement for fetching, not a replacement for notifications. As we reconfigure
the notification settings, this will be something we can consider. However, the scope of this change is to only activate
the existing IMAP IDLE service by default on the user's inbox if we can during account setup. Dramatic changes to
our settings, verbiage, and subsequent documentation is out of scope for this task, but will be part of "Phase 2" of
the notification settings improvement project.

## Outcome

