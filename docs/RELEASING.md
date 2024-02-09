# Create K-9 Mail releases

## One-time setup

1. Download `tb-android keystore` from 1Password and place it somewhere outside the root of the Git repository.
2. Add the following to `~/.gradle/gradle.properties` (create the file if necessary)
   ```
   k9mail.storeFile=<path to keystore>
   k9mail.storePassword=<password 'tb-android keystore' in 1Password>
   k9mail.keyAlias=k9mail
   k9mail.keyPassword=<password 'k9mail@tb-android' in 1Password>
   ```

### One-time setup for F-Droid builds

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

## Release a beta version

1. Update versionCode and versionName in `app-k9mail/build.gradle.kts`
2. Create change log entries in
   - `app/ui/legacy/src/main/res/raw/changelog_master.xml`
   - `app-k9mail/fastlane/metadata/android/en-US/changelogs/${versionCode}.txt`
     Use past tense. Try to keep them high level. Focus on the user (experience).
3. Commit the changes. Message: "Version $versionName"
4. Run `./gradlew clean :app-k9mail:assembleRelease --no-build-cache --no-configuration-cache`
5. Update an existing installation to make sure the app is signed with the proper key and runs on a real device.
   ```
   adb install -r app-k9mail/build/outputs/apk/release/app-k9mail-release.apk
   ```
6. Tag as $versionName, e.g. `6.508`
7. Copy `app-k9mail/build/outputs/apk/release/app-k9mail-release.apk` as `k9-${versionName}.apk` to Google Drive (MZLA
   Team > K9 > APKs)
8. Change versionName in `app-k9mail/build.gradle.kts` to next version name followed by `-SNAPSHOT`
9. Commit the changes. Message: "Prepare for version $newVersionName"
10. Update `gh-pages` branch with the new change log
11. Push `main` branch
12. Push tags
13. Push `gh-pages` branch

### Create release on GitHub

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

### Create release on F-Droid

1. Fetch the latest changes from the _fdroiddata_ repository.
2. Switch to a new branch in your copy of the _fdroiddata_ repository.
3. Edit `metadata/com.fsck.k9.yml` to create a new entry for the version you want to release. Usually it's copy & paste
   of the previous entry and adjusting `versionName`, `versionCode`, and `commit` (use the tag name).
   Leave `CurrentVersion` and `CurrentVersionCode` unchanged. Those specify which version is the stable/recommended
   build.
4. Commit the changes. Message: "Update K-9 Mail to $newVersionName"
5. Run `fdroid build --latest com.fsck.k9` to build the project using F-Droid's toolchain.
6. Push the changes to your fork of the _fdroiddata_ repository.
7. Open a merge request on Gitlab. (The message from the server after the push in the previous step should contain a
   URL)
8. Select the _App update_ template and fill it out.
9. Create merge request and the F-Droid team will do the rest.

### Create release on Google Play

1. Go to the [Google Play Console](https://play.google.com/console/)
2. Select the _K-9 Mail_ app
3. Click on _Open testing_ in the left sidebar
4. Click on _Create new release_
5. Upload the APK to _App bundles_
6. Fill out Release name (e.g. "$versionCode ($versionName)")
7. Fill out Release notes (copy from `app-k9mail/fastlane/metadata/android/en-US/changelogs/${versionCode}.txt`)
8. Click _Next_
9. Review the release
10. Start with a staged rollout (usually 20%) for production and full rollout for beta versions
11. On the Publishing overview page, click _Send change for review_
12. Wait for the review to complete
13. In case of a rejection, fix the issues and repeat the process
14. Once the review is complete, monitor the staged rollout for issues and increase the rollout percentage as necessary

### Troubleshooting

### F-Droid

If the app doesn't show up in the F-Droid client:

- Check the build cycle, maybe you just missed it and it will be available in the next cycle. (The cycle is usually every 5 days.)
- Check [F-Droid Status](https://fdroidstatus.org/status/fdroid) for any issues.
- Check [F-Droid Monitor](https://monitor.f-droid.org/builds) for any errors mentioning `com.fsck.k9`.
