# RFC: Add "Push" Notifications to New Users' Inboxes With IMAP IDLE

- Issue: [#11320](https://github.com/thunderbird/thunderbird-android/issues/11320)
- Status: **Proposed**

## Summary

When a new user creates an account, if it is an IMAP account and IMAP IDLE is supported on the server, we will turn on "push" syncing for the inbox folder using IMAP IDLE. If they choose to enable notifications, they'll receive notifications when they receive new emails in as close to instant as we can get it without a true push service.

## Motivation

Users complain that their emails do not show up on time. When they learn they can activate IMAP IDLE, which we call "push" to the end user, they find that the setting is buried in an unexpected place. They may also misconfigure it, causing their email to stop working altogether. While IMAP IDLE works on a per-folder basis, the end user doesn't want to have to think about this level of detail. They don't want to have to figure out server capacities for open sockets and pick their most important folders. They just want their new mail when it comes in. Therefore, we'll turn on IMAP IDLE on their Inbox, what they'll see as "push," which will give them a notification or instant syncing of their inbox when changes appear in that folder on the server. Most users will never feel the need to change this setting, and will believe it's working as it was always intended.

## Proposal

Whether or not a folder has push enabled is a boolean stored locally in `FolderDetails.isPushEnabled`, with those details saved to the device's storage in `FolderSettingsDataStore`. We'd update the folder's details to contain `isPushEnabled = true` when creating a folder details instance for the account's inbox folder.

The exact process to create a new `FolderDetails` instance when generating a new account, and not any time an instance is created, would require further investigation. It will likely involve getting server capabilities in the `AccountCreator.create` function, checking for the `Capabilities.IDLE` server setting enabled in the `RealImapConnection.isIdleCapable` property, and then getting the specific Inbox folder (`LegacyAccount.inboxFolderId`) and enabling the `isPushEnabled` variable. However, we will still have to determine where is the most efficient way to do this in that it is set up during account creation, before the user's inbox has loaded, and does not require a great deal of dependency changes to make the information available. This will require further technical investigation.

## Alternatives Considered

Initially, we had hoped to enable IMAP IDLE on **all** folders that could support it. However, there are limits to the number of folders that can be synced through IDLE sockets with IMAP. The limit is server-specific, and some may fail silently. Others present an error that reports they have too many connections. An end user who isn't aware of the limitations of IMAP IDLE would not know what this means, and would therefore not be able to solve the issue without our help. The end result would be issues for most—if not all—users who set up an IMAP account using Thunderbird or K-9 that would completely disable their account until we can walk them through the many steps to fix it.

We _could_ solve this by including some information in the Folder Settings view on the limits of folder syncing, but the goal here is to reduce the need of users to change these settings at all.

By defaulting push notifications on for new users, they won't need to set up individual folders to sync because they'll get their messages when expected. If they have a more complex setup or want to increase the sync time of items in other folders, they can still do so manually. When we make this process easier later this year, we'll want to notify the user that activating push on too many folders may break their connection to the server. Changing these user-facing settings menus is out of scope for this specific project, but will be part of future notification settings improvements. We may potentially perform a fetch on all folders when we notice a change on the inbox, which would have the same effect as syncing with all folders as the message would likely first appear in their inbox before sorting anyway, but this may be an option we want to present to the user, as it could cause battery drain and use larger amounts of data than IMAP IDLE would alone.

## Risks & Drawbacks

IMAP IDLE keeps a socket open to receive information from the server when there are changes to the folders they're watching. This will be the Inbox folder. If a user receives many email messages throughout the day, it could potentially lead to more fetches, and more battery usage. In my personal testing, there seemed to be no difference in battery consumption between fetching all folders every 15 minutes and turning on IMAP IDLE for multiple accounts, and push on for multiple folders as well, more than our end users would get with the default settings proposed here. Users can turn the feature off if it becomes an issue, but IMAP IDLE was made specifically to reduce the need to sync with a server frequently and reduce both battery usage and network bandwidth. As a result, it may reduce battery consumption for our users. Those who see an increase would likely have an account where they receive many emails, but not all at once, such as automated emails every few minutes. Someone with such a setup could turn off the setting where it exists today. In a future project, we'll make these settings even easier to access.

## Open Questions

It's rather unfortunate that there's no way to be sure of how many sockets we can keep open for each server. Some are as little as 2-4, while others can support IMAP IDLE for 8 folders. There's no way to test this besides creating a failure state. We could do so, keep a list of the maximum number of connections and, if a user has fewer folders than that, enable IMAP IDLE syncing on each of their folders. However, a vast majority of our users will only need this on the inbox and perhaps, if they do server-side filtering, an "important" or "flagged" folder. But by doing a full fetch on each change on the Inbox, we may actually resolve this, as the emails will likely end up in the inbox first, before sorting, and then the filtering or automation they're doing could place the email elsewhere. Either way, we'd trigger a sync because we're waiting for updates to the inbox. We may want to revisit this idea later, but, for now, simply activating IMAP IDLE on the Inbox folder may be sufficient to solve user complaints.

## Outcome

