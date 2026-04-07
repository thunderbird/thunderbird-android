# Weblate CLI

This is a command line interface that will check the [weblate](https://hosted.weblate.org/projects/tb-android/#components) components configuration and apply a streamlined configuration.

## Usage

To use this script you need to have a [weblate token](https://hosted.weblate.org/accounts/profile/#api). You can get it by logging in to weblate and going to your profile settings.

You can run the script with the following command:

```bash
./scripts/weblate --token <weblate-token> [--dry-run]
```

It will patch all components to the same configuration.

If you want to preview the outcome you can pass the `--dry-run` argument. It will print out the changes that would be applied.

```bash
./scripts/weblate --token <weblate-token> --dry-run
```
