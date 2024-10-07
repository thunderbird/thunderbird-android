# Release Automation Setup

Release automation is triggered by the workflow_dispatch event on the "Shippable Build & Signing"
workflow.

GitHub environments are used to set configuration variables for each application
and release type. The environment is selected when triggering the workflow. You must
also select the appropriate branch to run the workflow on. The environments are only
accessible by the branch they are associated with

## Build Environments

- thunderbird_beta
- thunderbird_daily
- thunderbird_release
- thunderbird_debug

The variables set in these environments are non-sensitive and are used by the build job.

- APP_NAME: app-thunderbird | app-k9
- TAG_PREFIX: THUNDERBIRD | K9MAIL
- RELEASE_TYPE: debug | daily | beta | release
- MATRIX_INCLUDE:
  - This is a JSON string used to create the jobs matrix. For example, for
    Thunderbird beta, the (YAML) value would be:
  ```yaml
  - packageFormat: bundle
    packageFlavor: full
  - packageFormat: apk
    packageFlavor: foss
  ```
  That would build `bundleFullBeta` and `assembleFossBeta`.

## Signing Environments

There are also "secret" environments that are used by the signing job.

An "upload" secret environment and a "signing" secret environment are needed. Currently the environment names are based
on the appName, releaseType, and packageFlavor. So `app-thunderbird_beta_full` which would have the upload
signing configuration for Thunderbird Beta set up. This could be improved.
The secrets themselves are from https://github.com/noriban/sign-android-release:

```yaml
signingKey: ${{ secrets.SIGNING_KEY }}
alias: ${{ secrets.KEY_ALIAS }}
keyPassword: ${{ secrets.KEY_PASSWORD }}
keyStorePassword: ${{ secrets.KEY_STORE_PASSWORD }}
```

## Publishing Hold Environment

The "publish_hold" is shared by all application variants and is used by the "pre_publish" job.
It has no secrets or variables, but "Required Reviewers" is set to trusted team members who oversee releases. The
effect is that after package signing completes, the publishing jobs that depend on it will not run until released
manually.

![publish hold](publish_hold.png)

## Github Releases Environment

"gh_releases" contains the Client Id and Private Key for a Github App that's used by the "actions/create-github-app-token'
to generate a token with the appropriate permissions to create and tag a Github release.

|          | Name                     | Description                     |
| -------- | ------------------------ | ------------------------------- |
| Variable | RELEASER_APP_CLIENT_ID   | The Client ID of the github app |
| Secret   | RELEASER_APP_PRIVATE_KEY | The private key of the app      |

### App Permissions

**TODO**
