### Release Manual Testing Checklist

Purpose

- Ensure consistent, repeatable pre-release validation across core areas: installation/upgrade, UI/UX, functionality regression, performance, connectivity, and security/privacy.
- Provide a short “Smoke” run and a full “Release” run.

When to use

- Before every Beta/Production release
- After high‑risk feature merges

Test environments

- Devices: At least one low-end and one mid/high-end device; 1 emulator
- OS versions: Last 3 major Android versions supported + Lowest supported (API 21)
- Network: Wi‑Fi, LTE/3G, Offline/Airplane; captive portal if available
- Accounts: At least 2 IMAP accounts (different providers), 1 POP3 if supported; one account with many messages/folders
- Locales: English + one non‑English (RTL if possible)
- Themes: Light and Dark

Data prerequisites

- Test mailboxes with folders: Inbox, Sent, Drafts, Trash, Spam, Archive, custom folders
- A few threads with 50+ messages for performance/scroll tests
- At least one account configured with push (if supported)

How to record results

- Mark each item Pass/Fail/Blocked/Not Applicable
- Record build/commit, device, OS, account provider, network type
- File defects with clear reproduction steps, logs, and screenshots

## Quick Smoke (15–25 min)

1. Install/Launch
   - Fresh install: App installs and launches without crash
   - First‑run: Onboarding visible, no ANRs
2. Account Setup (1 IMAP)
   - Manual or auto‑config completes; inbox loads
3. UI Smoke
   - Navigate: Account list → Inbox → Message → Back works
   - Compose: Create draft, send to self, appears in Sent and then Inbox
4. Sync/Refresh
   - Pull‑to‑refresh fetches new mail; background sync triggers at expected times
5. Notifications
   - Receive mail notification; tap opens message; mark as read behavior correct
6. Basic Actions
   - Read, star/flag, delete to Trash; undo/restore if offered
7. Performance quick check
   - App cold start acceptable, scrolling in Inbox smooth

## Full Release Checklist

### A. Installation and Upgrade

Top-level verification

- Fresh Install — App installs and runs correctly on a fresh install
  - Installs and launches without crash
  - Permissions requested only when needed; rationale shown
  - First‑run experience shows expected screens/toggles
- Upgrade to Production — App upgrades correctly to the latest production version (only for releases)
  - Upgrade to latest production build preserves accounts, settings, and local cache
  - No duplicate notifications or migration errors on first launch
- Upgrade to Beta — App upgrades correctly to the latest beta version
  - Upgrade to latest beta build preserves accounts, settings, and local cache
  - Feature flags and migrations behave correctly when upgrading to beta
  - No duplicate notifications or migration errors on first launch
- Backup/Restore
  - Backup 1 account setting, delete account from app, restore from backup; account usable
  - Backup all accounts, uninstall/reinstall, restore settings; accounts usable
- Settings Import via QR code (only Thunderbird)
  - From first-run flow, choose "Import settings"; scan a valid QR; settings are imported/applied or account is added/usable
  - From Settings > Import, choose "Scan QR code"; scan a valid QR; settings are imported/applied or account is added/usable
  - Try an invalid or expired QR; a clear error is shown and the app does not crash

### B. Account Management

- Add accounts
  - Auto‑config works for common providers; fallback to manual works
  - Validation errors are clear (username/password/server settings)
- Multiple accounts
  - Switch accounts; titles/subtitles update correctly
  - Unified Inbox visibility matches setting and number of accounts
- Delete account
  - Deleting current account transitions correctly (Unified Inbox if enabled and >1; else default account)
  - No crash during or after deletion; capability checks safe
- Special folders
  - Drafts/Sent/Archive/Trash/Spam configured/created as needed

## C. Core Mail Flows (per account)

- Receiving
  - New message appears after push/refresh; badge/notification updates
- Reading
  - Open message; images loading policy obeyed; external images prompt if applicable
- Compose/Reply/Forward
  - From identity correct; quoted content; signatures; attachments; send succeeds
- Drafts
  - Auto‑save draft; edit and send later
- Move/Copy
  - Move/copy messages between folders; operation succeeds and reflects in UI
- Search
  - Local search by sender/subject/body; filters; results correct; remote search if supported
- Attachments
  - Download, open with external app, share; large attachment handling

## D. UI and UX

- Navigation
  - Drawer/tabs/back behavior consistent; deep linking from notifications
- Message List view
  - Threaded/flat messages shown; selection mode; swipe actions (customizable to check archive/delete)
- Message view
  - Rendering of HTML/plain text; long messages; quoted text expansion; link handling
  - Image loading per settings; attachment previews
- Theming and localization
  - Light/Dark theme correctness; typography and icons;
  - Non‑English locale strings fit and are translated; RTL layout alignment
- Accessibility
  - TalkBack announcements; content descriptions present
  - Focus order logical; dynamic type/Font scale respected

## E. Performance and Resource Use

- Startup performance
  - Cold start time acceptable on low‑end device
- Scrolling and list operations
  - Smooth scrolling in Inbox with 1000+ items; no jank during load more
- Sync performance
  - Time to first sync for a fresh account reasonable; no repeated retries
- Resource usage
  - No abnormal CPU usage when idle; memory stable during long scrolling; cache does not balloon unexpectedly

## F. Connectivity and Background Behavior

- Network transitions
  - Wi‑Fi → Cellular → Offline: app handles gracefully; shows appropriate banners/status
- Offline usage
  - Read previously synced messages; queued actions (send/move) execute on reconnect

## G. Notifications

- New mail
  - Shows correct sender/subject; grouping for multiple messages
  - Actions: Mark read, Delete, Archive function correctly and reflect in app
- Quiet hours/Do Not Disturb
  - Respected according to settings

## H. Settings and Preferences

- General settings
  - Unified Inbox toggle behavior: only shows when >1 accounts, correct fallback behavior
  - Sort options persist per account/unified; changes reflected immediately
- Account settings
  - Outgoing/incoming server edits persist; re‑auth flows; OAuth if applicable

## I. Security and Privacy

- Permissions
  - No unexpected permission prompts; revoking a permission shows clear error states
- Sensitive logging
  - Disabled by default; enabling shows warnings; logs redact personal data where possible
- Certificates (if applicable)
  - Certificate errors surfaced with clear options; pinning remembered

## J. Regression Checks (recently changed areas)

- Check changes in this release
  - Perform smoke tests around changed screens/use cases
- Logs, crashes, and ANRs
  - No new crashes/ANRs; logging remains appropriate and doesn’t expose sensitive data
- Quick smoke around changed screens
  - Basic open/close, refresh/sync, action buttons, and navigation work

K. Known Issues Verification
- Validate that previously documented known issues still match behavior and are noted in release notes

## Sign-off

- All “Smoke” items: Pass
- No Critical/High defects open; Medium has workarounds; Low acceptable
- Performance thresholds met on target devices

## Appendix A: Quick result template

- Build/Commit:
- Device/OS:
- Accounts:
- Network:
- Locale/Theme:
- Summary:
- Defects filed:

## Appendix B: Full run log template

- Include timestamps, steps, expected vs actual, screenshots, and logs as needed

