# Maintenance patches

Once a maintenance branch was created (see [RELEASING.md]) and the first production release has been published, we try
not to touch the maintenance branch when it can be avoided. The changes that are applied to the maintenance branch are
usually fixing unintentional regressions and (new) bugs that negatively impact a lot of users.

## How to apply maintenance patches

Whenever possible, changes that are meant to be applied to the maintenance branch should be tested in a beta first.
Since beta versions are built from the `main` branch, such patches should land in `main` first. To keep track of these
patches, we use the "task: backport" label on pull requests introducing such changes.

After the change has been successfully tested in a beta, we create a new pull request to merge it into the maintenance
branch.

Step by step guide:

1. If possible, create a pull request to apply the change to `main`. If not, create a pull request against the
   maintenance branch and very carefully test and review such a change. Then go to step 7.
2. Add the "task: backport" label to the pull request.
3. After the pull request has been merged, release a beta version containing the change.
4. If the change doesn't work as intended for beta users, refine the change and go back to step 1.
5. Create a pull request to merge the change into the maintenance branch. This might require manual changes due to the
   branches having diverged since the maintenance branch was created. `git cherry-pick` usually works well enough.
6. Reference the pull request introducing the change to the `main` branch and remove the "task: backport" label from
   that pull request.
7. Check if there are remaining pull requests labeled with
   [task: backport](https://github.com/thunderbird/thunderbird-android/pulls?q=is%3Apr+label%3A%22task%3A+backport%22)
   and evaluate whether those should be backported before creating a new maintenance/stable release.
