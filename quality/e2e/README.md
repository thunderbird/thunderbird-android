# UI Flows - Maestro UI Automation Tests

[Maestro](https://maestro.dev/)-based UI automation for Thunderbird Android and K-9 Mail.

## Usage

Run all commands from the `quality/e2e` directory.

### Launching Tests

**Standard (Demo account)**:
```bash
./launch_tfa_debug.sh test tests/init_with_account.yml
```

**Using specific demo account**:
```bash
./launch_tfa_debug.sh --demo --email "user42@example.com" --account-name "Work" --user-name "User42" test tests/init_with_account.yml
```

**Using a real account**:
```bash
./launch_tfa_debug.sh --real test tests/init_with_account.yml
```

**Custom credentials (Real account)**:
```bash
./launch_tfa_debug.sh --real --email "user@example.com" --account-name "Work" --user-name "User42" test tests/init_with_account.yml
```

### Supported Variants

Each script targets a specific build variant:

- `launch_tfa_debug.sh` (Thunderbird Debug)
- `launch_tfa_daily.sh` (Thunderbird Daily)
- `launch_tfa_beta.sh` (Thunderbird Beta)
- `launch_tfa_release.sh` (Thunderbird Release)
- `launch_k9_debug.sh` (K-9 Mail Debug)
- `launch_k9_release.sh` (K-9 Mail Release)

All scripts support the `--real`, `--email`, `--account-name`, and `--user-name` flags. Backward-compat alias: `--display-name` maps to `--user-name`.

## Project Structure

- `scripts/`: Environment helpers (e.g., `detect_variant.js` for account property mapping).
- `setup/`: Centralized setup (only `handle_permissions.yml` for common entry point).
- `flows/`: Reusable, task-focused flows (e.g., `open_compose.yml`, `account_setup_demo.yml`).
- `tests/`: Thin end-to-end specs, prefixed by feature (e.g., `mail_compose.yml`, `account_onboarding.yml`).

## Architecture: Reusable Flows

We use small, task-focused flows that represent a single logical step or interaction (e.g., "Welcome screen" 
or "Fill compose form"). This approach keeps tests modular, avoids duplication, and makes them easy to 
assemble into complex scenarios.

- Flows may include both state checks (assertions) and interactions.
- Keep flows cohesive and focused (e.g., `open_compose.yml`, `fill_compose.yml`, `send_compose.yml`).

### Example

Compose email sequence uses three flows:

```yaml
- runFlow: { file: ../flows/mail/composer_open.yml }
- runFlow:
    file: ../flows/mail/composer_fill.yml
    env:
      TO_EMAIL: ${TO_EMAIL}
      SUBJECT: ${SUBJECT}
      BODY: ${BODY}
      IDENTITY: ${IDENTITY}
- runFlow: { file: ../flows/mail/composer_send.yml }
```

## Adding Tests

### 1. Create a Flow
Add a file in `flows/`, e.g., `flows/mail_fill_new_feature.yml`:
```yaml
appId: ${MAESTRO_APP_ID}
---
- tapOn: { id: "feature.button" }
- inputText: ${INPUT_VALUE}
```

### 2. Use it in a Test
Create `tests/mail_test_feature.yml`:
```yaml
appId: ${MAESTRO_APP_ID}
---
- runFlow:
    file: ../flows/mail/mail_fill_new_feature.yml
    env: { INPUT_VALUE: "Test Data" }
```

## Setup & Requirements

- **Device**: Android Emulator (Pixel 5 recommended, API 31+).
- **Language**: System language must be set to **English (US)**.
- **Maestro CLI**: [Install guide](https://docs.maestro.dev/getting-started/installing-maestro).

```bash
curl -Ls "https://get.maestro.dev" | bash
```

## Guidelines

- Use IDs: Prefer `id: "..."` selectors over text.
- Small flows: One task per flow; keep them short and reusable.
- Document inputs: List required env variables at the top of the YAML file.
- No hardcoding: Use `${MAESTRO_APP_ID}` instead of literal package names.
- Clean state: Use `clearState: true` in `launchApp` for reproducible scenarios (as seen in `tests/init_with_account.yml`).

## Environment Variables

These can be set via flags in the launch scripts:

| Variable | Flag | Default |
|---|---|---|
| `MAESTRO_ACCOUNT_TYPE` | `--real` | `demo` |
| `MAESTRO_EMAIL_ADDRESS` | `--email` | (variant specific) |
| `MAESTRO_ACCOUNT_NAME` | `--account-name` | `Demo Account` |
| `MAESTRO_USER_NAME` | `--user-name` (alias: `--display-name`) | `Demo User` |

## Resources

- [Maestro Docs](https://docs.maestro.dev/)
- [Maestro YAML Reference](https://docs.maestro.dev/api-reference/commands)
