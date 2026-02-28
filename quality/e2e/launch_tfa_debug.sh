#!/usr/bin/env bash

# Thunderbird Android Debug Launcher
# Usage: ./launch_tfa_debug.sh [--real] [--email EMAIL] [--account-name NAME] [--display-name NAME] test <scenario-file>
# Defaults to demo account. Use --real flag for real/configured account.
#
# Examples:
#   ./launch_tfa_debug.sh test scenarios/init_with_account.yml
#   ./launch_tfa_debug.sh --real test scenarios/init_with_account.yml
#   ./launch_tfa_debug.sh --real --email you@example.com test scenarios/init_with_account.yml

export MAESTRO_APP_ID="net.thunderbird.android.debug"
source "$(dirname "$0")/scripts/maestro_launcher.sh" "$@"
