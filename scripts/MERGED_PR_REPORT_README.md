# Merged PR Report

Generate a monthly report of pull requests merged into the `main`, `beta`, and `release` branches of `thunderbird/thunderbird-android`.

The script `scripts/merged-pr-report.sh` produces:

- A Markdown report for review
- A CSV report for spreadsheet import or further processing

## What it reports

For each merged pull request, the report includes:

- Target branch (`main`, `beta`, `release`)
- PR number
- Merge date
- PR title
- Report status from GitHub labels
- First beta tag containing the merge commit
- First release tag containing the merge commit

The CSV report additionally includes:

- PR author
- Merge commit SHA
- PR URL
- Empty `Comment` column for manual notes

## Report status labels

The script reads these labels from the PR:

| Label | Result |
|------|--------|
| `report: include` | Include |
| `report: exclude` | Exclude |
| `report: highlight` | Highlight |
| *(none)* | Review |

## Beta and Release columns

For each PR merge commit, the script determines:

- **Beta**: first beta tag (containing `b`) that contains the merge commit (only if the commit reached the `beta` branch)
- **Release**: first production release tag (excluding `b`, including dot releases) that contains the merge commit (only if the commit reached the `release` branch)

Possible values:

- A tag (e.g. `THUNDERBIRD_115_0b1`, `THUNDERBIRD_115_1_0`)
- `Not released yet` (commit reached the branch but is not yet tagged)
- `-` (commit not present in that branch history)

## How it works

The script:

1. Validates input (`YEAR`, `MONTH`)
2. Computes the monthly date range
3. Creates a temporary git repository
4. Fetches:
    - `main`, `beta`, `release`
    - all tags
5. Queries GitHub for merged PRs
6. Maps report labels to status
7. Resolves beta/release versions via git ancestry and tags
8. Writes Markdown and CSV outputs

To improve performance, version lookups are cached per merge commit SHA.

## Usage

```bash
./scripts/merged-pr-report.sh YEAR MONTH [TARGET_DIR] [--skip-excluded]
```

Example:

```bash
./scripts/merged-pr-report.sh 2026 02
./scripts/merged-pr-report.sh 2026 02 ./reports
./scripts/merged-pr-report.sh 2026 02 . --skip-excluded
```

Arguments:

- `YEAR`: Four-digit year (e.g. 2026)
- `MONTH`: Two-digit month (01-12)
- `TARGET_DIR`: (Optional) Target directory for reports (default: current directory)
- `--skip-excluded`: (Optional) If set, PRs with `report: exclude` label are omitted from the report

## Requirements

- `git`
- `gh` (GitHub CLI)
- `jq`
- macOS / BSD `date` (uses `date -j`)

Authenticate GitHub CLI:

```bash
gh auth login
