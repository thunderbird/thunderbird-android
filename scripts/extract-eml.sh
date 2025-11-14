#!/usr/bin/env bash
set -euo pipefail

# Extract exported .eml files from a connected Android device using adb.
#
# Default behavior: pull all .eml files from the Downloads folder into a local directory ./eml-files, preserving
# filenames. If a filename already exists locally, it will be overwritten. After a successful pull, the file is removed
# from the device by default.
#
# To keep files on the device, add --keep|-k to the command line.
#
# Requirements:
# - adb must be installed and a device connected with USB debugging enabled
# - The .eml files should have been exported on-device via "Export as EML" into Downloads.
#
# Usage:
#   scripts/extract-eml.sh [--keep|-k]
#     Pulls .eml files into ./eml-files (fixed target directory).
#     If --keep|-k is specified, files are not deleted from the device after pulling.

TARGET_DIR="./eml-files"
SOURCE_DIRS=(
  "/sdcard/Download"
  "/storage/emulated/0/Download"
)

# Options
KEEP=0
while [[ $# -gt 0 ]]; do
  case "$1" in
    -k|--keep)
      KEEP=1
      shift
      ;;
    -h|--help)
      echo "Usage: scripts/extract-eml.sh [--keep|-k]"
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      echo "Usage: scripts/extract-eml.sh [--keep|-k]" >&2
      exit 2
      ;;
  esac
done

mkdir -p "${TARGET_DIR}"

echo "Waiting for device..."
adb wait-for-device

if ! adb get-state >/dev/null 2>&1; then
  echo "Error: No device detected by adb. Make sure USB debugging is enabled and run 'adb devices'." >&2
  exit 1
fi

EML_LIST=$(adb shell ls /sdcard/Download/*.eml 2>/dev/null || true)

if [[ -z "${EML_LIST}" ]]; then
  echo "No .eml files found on device in: ${SOURCE_DIRS[*]}"
  exit 0
fi

COUNT=0
DELETED=0

while IFS= read -r REMOTE_PATH; do
  [[ -z "${REMOTE_PATH}" ]] && continue
  BASENAME=$(basename "${REMOTE_PATH}")
  DEST_PATH="${TARGET_DIR}/${BASENAME}"

  echo "Pulling (overwriting if exists): ${REMOTE_PATH} -> ${DEST_PATH}"
  if ! adb pull "${REMOTE_PATH}" "${DEST_PATH}" >/dev/null; then
    echo "Warning: Failed to pull ${REMOTE_PATH}" >&2
    continue
  fi
  ((COUNT++)) || true

  if [[ "${KEEP}" != "1" ]]; then
    if adb shell rm -f "${REMOTE_PATH}" >/dev/null 2>&1; then
      ((DELETED++)) || true
      echo "Deleted on device: ${REMOTE_PATH}"
    else
      echo "Warning: Failed to delete on device: ${REMOTE_PATH}" >&2
    fi
  fi

done < <(printf '%s\n' "${EML_LIST}")

if [[ ${COUNT} -gt 0 ]]; then
  if [[ "${KEEP}" != "1" ]]; then
    echo "Done. Pulled ${COUNT} file(s) into ${TARGET_DIR} and deleted ${DELETED} on device."
  else
    echo "Done. Pulled ${COUNT} file(s) into ${TARGET_DIR}. (Kept files on device)"
  fi
else
  echo "No files pulled."
fi

