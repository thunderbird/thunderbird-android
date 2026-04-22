# Weblate CLI

This is a command line interface that inspects Weblate project components and applies a
"golden" component configuration. It's intended for maintainers to review component configuration
consistency and, when appropriate, patch components to match the golden config.

## Usage

You need a Weblate API token (available from your Weblate account profile). A convenience wrapper script
is provided at `./scripts/weblate` which builds and runs the CLI.

Basic examples:

```bash
# Dry-run using the default golden config and managed components file
./scripts/weblate --token YOUR_WEBLATE_TOKEN --dry-run update

# Apply changes to managed components
./scripts/weblate --token YOUR_WEBLATE_TOKEN update

# Use a custom managed components file and golden config
./scripts/weblate --token YOUR_WEBLATE_TOKEN --managed-components-file ./cli/weblate-cli/managed-components.txt --golden-config-path ./cli/weblate-cli/golden-component-config.json --dry-run update
```

## Defaults

- Golden config: `./cli/weblate-cli/golden-component-config.json`
- Managed components file: `./cli/weblate-cli/managed-components.txt`

## Managed components file format

- One component slug per non-empty line. Inline comments are allowed after `#` and full-line comments that
  start with `#` are ignored.
- Matching is exact and case-sensitive against the component slug returned by the Weblate API.

Example:

```
# legacy
app-strings # ID: 17093 (main)
designsystem # ID: 25913
app-common
```

## Safety notes

- Always run with `--dry-run` first to verify diffs before applying changes to the live Weblate instance.

