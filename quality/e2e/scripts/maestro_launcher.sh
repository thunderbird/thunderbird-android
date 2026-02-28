#!/usr/bin/env bash
# Common Maestro Launcher Logic
# defaulting to demo account type

if [[ -z "$MAESTRO_APP_ID" ]]; then
  echo "Error: MAESTRO_APP_ID not set"
  exit 1
fi

export MAESTRO_ACCOUNT_TYPE="demo"

# Parse optional flags
SCENARIO_FILE=""
while [[ $# -gt 0 ]]; do
  case $1 in
    --demo)
      export MAESTRO_ACCOUNT_TYPE="demo"
      shift
      ;;
    --real)
      export MAESTRO_ACCOUNT_TYPE="real"
      shift
      ;;
    --email)
      export MAESTRO_EMAIL_ADDRESS="$2"
      shift 2
      ;;
    --account-name)
      export MAESTRO_ACCOUNT_NAME="$2"
      shift 2
      ;;
    --user-name)
      export MAESTRO_USER_NAME="$2"
      shift 2
      ;;
    test)
      shift
      SCENARIO_FILE="$1"
      shift
      ;;
    *)
      shift
      ;;
  esac
done

if [[ -z "$SCENARIO_FILE" ]]; then
  echo "Error: scenario file not provided"
  echo "Usage: $0 [--demo] [--real] [--email EMAIL] [--account-name NAME] [--user-name NAME] test <scenario-file>"
  exit 1
fi

export MAESTRO_APP_ID
export MAESTRO_ACCOUNT_TYPE
export MAESTRO_EMAIL_ADDRESS
export MAESTRO_ACCOUNT_NAME
export MAESTRO_USER_NAME

maestro test "$SCENARIO_FILE"
