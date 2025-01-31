# Releases

Thunderbird for Android follows a release train model to ensure timely and predictable releases. This model allows for regular feature rollouts, stability improvements, and bug fixes.

## Branches in the Release Train Model

### Daily

Daily builds are used for initial testing of new features and changes. Feature flags are additionally used to work on features that are not yet ready for consumption.

- **Branch:** `main`
- **Purpose:** Active development of new features and improvements
- **Release Cadence:** Daily
- **Audience:** Developers and highly technical users who want to test the bleeding edge of Thunderbird. Nightly builds are unstable and not recommended for daily use.
- **Availability:** Daily builds are available on the Play Store internal channel. APKs are available on ftp.mozilla.org.

### Beta

After features are stabilized in Daily, they are merged into the Beta branch for broader testing. Uplifts are limited to bug/security fixes only. The Beta branch serves as a preview of what will be included in the next stable release, allowing for user feedback and final adjustments before general availability.

- **Branch:** `beta`
- **Purpose:** Pre-release testing
- **Release Cadence:** Weekly with the option to skip if not needed
- **Merge Cadence:** Every 2 Months
- **Audience:** Early adopters and testers. Testers are encouraged to provide error logs and help reproduce issues filed.
- **Availability:** Beta builds are available from the [Play Store](https://play.google.com/store/apps/details?id=net.thunderbird.android.beta) and [F-Droid](https://f-droid.org/packages/net.thunderbird.android.beta).

### Release

This branch represents the stable version of Thunderbird, which is released to the public. It is tested and suitable for general use. Bug fixes and minor updates are periodically applied between major releases. Uplifts to Release are limited to stability/security fixes only.

- **Branch:** `release`
- **Purpose:** Stable releases
- **Release Cadence:** Major releases every 2 months. Minor releases every 2 weeks with the option to skip if not needed.
- **Merge Cadence:** Every 2 months
- **Audience:** General users. Users may be filing bug reports or leaving reviews to express their level of satisfaction.
- **Availability:** Release builds are available from the [Play Store](https://play.google.com/store/apps/details?id=net.thunderbird.android) and [F-Droid](https://f-droid.org/packages/net.thunderbird.android).

## Example Feature Release Flow

1. A new feature is developed and merged via pull requests into the `main` branch.
2. Every 2 months, `main` is merged into the `beta` branch for external testing and feedback.
3. Every 2 months, `beta` is merged into the `release` branch, and a release is made available to all users.

## Example Bug Release Flow

1. A high-impact bug is fixed and merged via pull request into the `main` branch.
2. After it has received adequate testing on `daily`, the fix is cherry-picked (uplifted) to the `beta` branch and released in the next scheduled beta.
3. After it has received adequate testing on `beta`, the fix is cherry-picked (uplifted) to the `release` branch and released in the next stable minor release.

## Sample Release Timeline

| Milestone                      | Details   | Date   |
| ------------------------------ | --------- | ------ |
| TfA 11.0a1 starts              |           | Feb 28 |
| TfA merge 11.0a1 main->beta    |           | May 2  |
| TfA 11.0b1                     |           | May 5  |
| TfA 11.0bX                     | If needed | May 12 |
| TfA 11.0bX                     | If needed | May 19 |
| TfA 11.0bX                     | If needed | May 26 |
| TfA 11.0bX                     | If needed | Jun 2  |
| TfA 11.0bX                     | If needed | Jun 9  |
| TfA 11.0bX                     | If needed | Jun 16 |
| TfA 11.0bX                     | If needed | Jun 23 |
| TfA 11.0bX                     | If needed | Jun 30 |
| TfA merge 11.0bX beta->release |           | Jun 30 |
| TfA 11.0                       |           | Jul 7  |
| TfA 11.X                       | If needed | Jul 21 |
| TfA 11.X                       | If needed | Aug 4  |
| TfA 11.X                       | If needed | Aug 18 |
| TfA 11.X                       | If needed | Sep 1  |

## Feature Flags

Thunderbird for Android uses Feature Flags to disable features not yet ready for consumption.

- On `main`, feature flags are enabled as soon as developers have completed all pull requests related to the feature.
- On `beta`, feature flags remain enabled unless the feature has not been fully completed and the developers would like to pause the feature.
- On `release`, feature flags are disabled until an explicit decision has been made to enable the feature for all users.

## Versioning System

### Version Names

Thunderbird for Android stable release versions follow the `X.Y` format, where:

- **X (Major version):** Incremented for each new release cycle.
- **Y (Patch version):** Incremented when changes are added to an existing major version.

For beta builds, the suffix `b1` is appended, where the number increments for each beta. For daily builds, the suffix `a1` is appended, which remains constant.

### Version Codes

The version code is an internal version number for Android that helps determine whether one version is more recent than another.

The version code for beta and release is an integer value that increments for each new release.

The version code for daily is calculated based on the date and has the format `yDDDHHmm`:

- **y**: The number of years since a base year, with 2023 as the starting point (e.g., 2024 is 1)
- **DDD**: The day of the year in 3 digits, zero-padded
- **HH**: The hour of the day in 2 digits (00–23)
- **mm**: The minute of the hour in 2 digits

For example:

- `2024-02-09 16:45` → `1 | 040 | 16 | 45` → `10401645`
- `2025-10-12 09:23` → `2 | 285 | 09 | 23` → `22850923`
- `2122-02-09 16:45` → `99 | 040 | 16 | 45` → `990401645`

## Merge Days

Active development occurs on the `main` branch and becomes part of daily. Every 2 months:

1. `main` is merged into `beta`, for testing.
2. `beta` is merged into `release`, making it publicly available.

On the former, `main` carries over to `beta`, where the community can test the changes as part of “Thunderbird Beta for Testers” (`net.thunderbird.android.beta`) until the next merge day.
On the latter, code that was in beta goes to release, where the general population receives product updates (`net.thunderbird.android`).

When a merge occurs, the version name is carried forward to the next branch. However, the alpha and beta suffixes are removed/reset accordingly. For example, let’s say we are shortly before the Thunderbird 9.0 release. The latest releases were Thunderbird 8.2, Thunderbird Beta 9.0b4, and Thunderbird Daily 10.0a1. Here is what happens:

- The `beta` branch is merged to `release`. The resulting version on release changes from 8.2 to 9.0.
- The `main` branch is merged to `beta`. The resulting version on beta changes from 9.0b4 to 10.0b1
- The `main` branch version number is changed from 10.0a1 to 11.0a1

While the version name changes, it must be ensured that the version code stays the same for each branch. Our application IDs are specific to the branch they are on. For example:

- Beta always uses `net.thunderbird.android.beta` as the app ID. Let's say the version code is 20 at 9.0b4, it will be 21 at 10.0b1.
- Likewise, when 9.0b4 becomes 9.0, if the version code on beta is 20 and on release it is 12, then 9.0 becomes 13 and not 21.

## Merge Process

Merges are performed with the `git merge` command:

```sh
 git checkout beta
 git merge main
```

This approach enables various benefits, including:

- Carrying forward main branch history to beta, and beta branch history to release.
- No branch history is lost.
- Git tags are retained in git log.
- Files/code that is unique per branch can remain that way (e.g. notes files such as changelog_master.xml, version codes).

## Branch Uplifts

If the urgency of a fix requires it to be included in the Beta or Release channel before the next merge, the uplift process is followed. If possible, uplifts should be avoided and patches should “ride the train” instead, following the merge day cycle.

### Uplift Criteria

Beta uplifts should:

- Be limited to bug/security fixes only (features ride the train).
- Not change any localizable strings
- Have tests, or a strong statement of what can be done in the absence of tests.
- Have landed in main and stabilized on the daily channel.
- Have a comment in the GitHub issue assessing performance impact, risk, and reasons the patch is needed on beta.

Release uplifts should additionally:

- Be limited to stability/security fixes only (features ride the train).
- Have landed in beta and stabilized on the beta channel.

### Uplift Process

1. The requestor creates a pull request to the respective target branch with the cherry-picked commits they intend to uplift.
2. The requestor makes a comment in the bug with the Approval Request Comment template filled out.
3. The requestor includes a link to the pull request in the approval request comment and sets the `uplift-approval?` label.
4. The release driver reviews pull requests with the `uplift-approval?` label, and will merge any pull requests that are approved, or close any pull requests that are declined.

Uplift patches are generated with:

```sh
git cherry-pick -x <commit-hash>
```

Template for uplift requests:

```sh
[Approval Request Comment]
Original Issue/Pull request:
Regression caused by (issue #):
User impact if declined:
Testing completed (on daily, etc.):
Risk to taking this patch (and alternatives if risky):
```

## Create K-9 Mail releases

### One-time setup

1. Create a `.signing` folder in the root of the Git repository, if it doesn't exist yet.
2. Download the `k9-release-signing.jks` and `k9.release.signing.properties` files from 1Password and place them in the `.signing` folder.

Example `<app>.<realeaseType>.signing.properties` file:

```
<app>.<releaseType>.storeFile=<path to keystore '../.signing/k9mail.jks'>
<app>.<releaseType>.storePassword=<storePassword>
<app>.<releaseType>.keyAlias=<keyAlias>
<app>.<releaseType>.keyPassword=<keyPassword>
```

- `<app>` is the short name of the app, e.g. `k9`
- `<releaseType>` is the type of release, e.g. `release`

#### One-time setup for F-Droid builds

1. Install _fdroidserver_ by following
   the [installation instructions](https://f-droid.org/docs/Installing_the_Server_and_Repo_Tools).
   1. On MacOS, it's best to install the latest version from source, because the version in Homebrew has some issues.
      1. Install the android command line tools if not available already.
         ```
         brew install --cask android-commandlinetools
         ```
      2. Install latest _fdroidserver_ from source:
         ```
         python -m venv fdroidserver-env
         source fdroidserver-env/bin/activate
         pip install git+https://gitlab.com/fdroid/fdroidserver.git
         ```
      3. To use _fdroidserver_ from the command line, you need to activate the virtual environment before each use:
         ```
         source fdroidserver-env/bin/activate
         ```
      4. To deactivate the virtual environment:
         ```
         deactivate
         ```
2. [Sign up for a Gitlab account](https://gitlab.com/users/sign_up) and fork
   the [fdroiddata](https://gitlab.com/fdroid/fdroiddata) repository.
3. Clone your fork of the _fdroiddata_ repository.

### Release a beta version

1. Update versionCode and versionName in `app-k9mail/build.gradle.kts`
2. Create change log entries in
   - `app-k9mail/src/main/res/raw/changelog_master.xml`
   - `app-metadata/com.fsck.k9/en-US/changelogs/${versionCode}.txt`
     Use past tense. Try to keep them high level. Focus on the user (experience).
3. Update the metadata link to point to K-9 Mail's data:
   `ln --symbolic --no-dereference --force app-metadata/com.fsck.k9 metadata`
4. Commit the changes. Message: "Version $versionName"
5. Run `./gradlew clean :app-k9mail:assembleRelease --no-build-cache --no-configuration-cache`
6. Update an existing installation to make sure the app is signed with the proper key and runs on a real device.
   ```
   adb install -r app-k9mail/build/outputs/apk/release/app-k9mail-release.apk
   ```
7. Tag as $versionName, e.g. `6.508`
8. Copy `app-k9mail/build/outputs/apk/release/app-k9mail-release.apk` as `k9-${versionName}.apk` to Google Drive (MZLA
   Team > K9 > APKs)
9. Change versionName in `app-k9mail/build.gradle.kts` to next version name followed by `-SNAPSHOT`
10. Commit the changes. Message: "Prepare for version $newVersionName"
11. Update `gh-pages` branch with the new change log
12. Push `main` branch
13. Push tags
14. Push `gh-pages` branch

#### Create release on GitHub

1. Go to https://github.com/thunderbird/thunderbird-android/tags and select the appropriate tag
2. Click "Create release from tag"
3. Fill out the form
   - Click "Generate release notes"
   - Replace contents under "What's changed" with change log entries
   - Add GitHub handles in parentheses to change log entries
   - If necessary, add another entry "Internal changes" (or similar) so people who contributed changes outside of the
     entries mentioned in the change log can be mentioned via GitHub handle.
   - Attach the APK
   - Select "Set as a pre-release"
   - Click "Publish release"

#### Create release on F-Droid

1. Fetch the latest changes from the _fdroiddata_ repository.
2. Switch to a new branch in your copy of the _fdroiddata_ repository.
3. Edit `metadata/com.fsck.k9.yml` to create a new entry for the version you want to release. Usually it's copy & paste
   of the previous entry and adjusting `versionName`, `versionCode`, and `commit` (use the tag name).
   Leave `CurrentVersion` and `CurrentVersionCode` unchanged. Those specify which version is the stable/recommended
   build.

   Example:

   ```yaml
   - versionName: "${versionName}"
     versionCode: ${versionCode}
     commit: "${tagName}"
     subdir: app-k9mail
     gradle:
       - yes
     scandelete:
       - build-plugin/build
   ```

4. Commit the changes. Message: "Update K-9 Mail to $newVersionName (beta)"
5. Run `fdroid build --latest com.fsck.k9` to build the project using F-Droid's toolchain.
6. Push the changes to your fork of the _fdroiddata_ repository.
7. Open a merge request on Gitlab. (The message from the server after the push in the previous step should contain a
   URL)
8. Select the _App update_ template and fill it out.
9. Create merge request and the F-Droid team will do the rest.

#### Create release on Google Play

1. Go to the [Google Play Console](https://play.google.com/console/)
2. Select the _K-9 Mail_ app
3. Click on _Open testing_ in the left sidebar
4. Click on _Create new release_
5. Upload the APK to _App bundles_
6. Fill out Release name (e.g. "$versionCode ($versionName)")
7. Fill out Release notes (copy from `app-metadata/com.fsck.k9/en-US/changelogs/${versionCode}.txt`)
8. Click _Next_
9. Review the release
10. Configure a full rollout for beta versions
11. On the Publishing overview page, click _Send change for review_
12. Wait for the review to complete
13. In case of a rejection, fix the issues and repeat the process

### Release a stable version

When the team decides the `main` branch is stable enough and it's time to release a new stable version, create a new
maintenance branch (off `main`) using the desired version number with the last two digits dropped followed by `-MAINT`.
Example: `6.8-MAINT` when the first stable release is K-9 Mail 6.800.

Ideally the first stable release contains no code changes when compared to the last beta version built from `main`.
That way the new release won't contain any changes that weren't exposed to user testing in a beta version before.

1. Switch to the appropriate maintenance branch, e.g. `6.8-MAINT`
2. Update versionCode and versionName in `app-k9mail/build.gradle.kts` (stable releases use an even digit after the
   dot, e.g. `5.400`, `6.603`)
3. Create change log entries in
   - `app-k9mail/src/main/res/raw/changelog_master.xml`
   - `app-k9mail/fastlane/metadata/android/en-US/changelogs/${versionCode}.txt`
     Use past tense. Try to keep them high level. Focus on the user (experience).
4. Update the metadata link to point to K-9 Mail's data:
   `ln --symbolic --no-dereference --force app-metadata/com.fsck.k9 metadata`
5. Commit the changes. Message: "Version $versionName"
6. Run `./gradlew clean :app-k9mail:assembleRelease --no-build-cache --no-configuration-cache`
7. Update an existing installation to make sure the app is signed with the proper key and runs on a real device.
   ```
   adb install -r app-k9mail/build/outputs/apk/release/app-k9mail-release.apk
   ```
8. Tag as $versionName, e.g. `6.800`
9. Copy `app-k9mail/build/outputs/apk/release/app-k9mail-release.apk` as `k9-${versionName}.apk` to Google Drive (MZLA
   Team > K9 > APKs)
10. Update `gh-pages` branch with the new change log. Create a new file if it's the first stable release in a series.
11. Push maintenance branch
12. Push tags
13. Push `gh-pages` branch

#### Create release on GitHub

1. Go to https://github.com/thunderbird/thunderbird-android/tags and select the appropriate tag
2. Click "Create release from tag"
3. Fill out the form
   - Click "Generate release notes"
   - Replace contents under "What's changed" with change log entries
   - Add GitHub handles in parentheses to change log entries
   - If necessary, add another entry "Internal changes" (or similar) so people who contributed changes outside of the
     entries mentioned in the change log can be mentioned via GitHub handle.
   - Attach the APK
   - Select "Set as the latest release"
   - Click "Publish release"

#### Create release on F-Droid

1. Fetch the latest changes from the _fdroiddata_ repository.
2. Switch to a new branch in your copy of the _fdroiddata_ repository.
3. Edit `metadata/com.fsck.k9.yml` to create a new entry for the version you want to release. Usually it's copy & paste
   of the previous entry and adjusting `versionName`, `versionCode`, and `commit` (use the tag name).
   Change `CurrentVersion` and `CurrentVersionCode` to the new values, making this the new stable/recommended build.

   Example:

   ```yaml
   - versionName: "${versionName}"
     versionCode: ${versionCode}
     commit: "${tagName}"
     subdir: app-k9mail
     gradle:
       - yes
     scandelete:
       - build-plugin/build
   ```

4. Commit the changes. Message: "Update K-9 Mail to $newVersionName"
5. Run `fdroid build --latest com.fsck.k9` to build the project using F-Droid's toolchain.
6. Push the changes to your fork of the _fdroiddata_ repository.
7. Open a merge request on Gitlab. (The message from the server after the push in the previous step should contain a
   URL)
8. Select the _App update_ template and fill it out.
9. Create merge request and the F-Droid team will do the rest.

#### Create release on Google Play

1. Go to the [Google Play Console](https://play.google.com/console/)
2. Select the _K-9 Mail_ app
3. Click on _Production_ in the left sidebar
4. Click on _Create new release_
5. Upload the APK to _App bundles_
6. Fill out Release name (e.g. "$versionCode ($versionName)")
7. Fill out Release notes (copy from `app-k9mail/fastlane/metadata/android/en-US/changelogs/${versionCode}.txt`)
8. Click _Next_
9. Review the release
10. Start with a staged rollout (usually 20%)
11. On the Publishing overview page, click _Send change for review_
12. Wait for the review to complete
13. In case of a rejection, fix the issues and repeat the process
14. Once the review is complete, monitor the staged rollout for issues and increase the rollout percentage as necessary

### Troubleshooting

#### F-Droid

If the app doesn't show up in the F-Droid client:

- Check the build cycle, maybe you just missed it and it will be available in the next cycle. (The cycle is usually every 5 days.)
- Check [F-Droid Status](https://fdroidstatus.org/status/fdroid) for any issues.
- Check [F-Droid Monitor](https://monitor.f-droid.org/builds) for any errors mentioning `com.fsck.k9`.
