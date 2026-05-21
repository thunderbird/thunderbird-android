#!/bin/bash

## This script installs mdbook and extensions, additionally it downloads the latest Mermaid.js version.
## If the script is run with the "--force" argument, it will force the installation of mdbook and it's extensions.

set -e

# Define versions
MDBOOK_VERSION="0.5.2" # https://github.com/rust-lang/mdBook/releases
MDBOOK_LAST_CHANGED_VERSION="0.4.0" # https://github.com/badboy/mdbook-last-changed/releases
MDBOOK_MERMAID_VERSION="0.17.0" # https://github.com/badboy/mdbook-mermaid/releases
MERMAID_JS_VERSION="v11.14.0" # https://github.com/mermaid-js/mermaid/releases

# Define installation paths
BASE_DIR=$(dirname -- "${BASH_SOURCE[0]}")
MERMAID_JS_DIR="${BASE_DIR}/assets/additional/js/"
MERMAID_JS_PATH="${MERMAID_JS_DIR}mermaid.min.js"

# Check if the script was run with "force" argument
FORCE_UPDATE=false
if [ "$1" == "--force" ]; then
    FORCE_UPDATE=true
    echo "Force update mode enabled."
fi

# Ensure Cargo (Rust) is installed
if ! command -v cargo &> /dev/null; then
    echo "Cargo (Rust) is required to install mdbook"
    echo "Please install Rust from https://www.rust-lang.org/tools/install."
    exit 1
fi

# Install mdbook
if $FORCE_UPDATE; then
    echo "Forcing mdbook installation..."
    cargo install --force mdbook --version $MDBOOK_VERSION
    cargo install --force mdbook-last-changed --version $MDBOOK_LAST_CHANGED_VERSION
    cargo install --force mdbook-mermaid --version $MDBOOK_MERMAID_VERSION
else
    cargo install mdbook --version $MDBOOK_VERSION
    cargo install mdbook-last-changed --version $MDBOOK_LAST_CHANGED_VERSION
    cargo install mdbook-mermaid --version $MDBOOK_MERMAID_VERSION
fi

mkdir -p "$MERMAID_JS_DIR"

# Download Mermaid.js. If FORCE_UPDATE is enabled, always re-download/overwrite the file.
if $FORCE_UPDATE; then
    echo "Forcing Mermaid.js download of version ${MERMAID_JS_VERSION}..."
    curl -fL "https://cdn.jsdelivr.net/npm/mermaid@${MERMAID_JS_VERSION}/dist/mermaid.min.js" -o "$MERMAID_JS_PATH"
else
    if [ ! -f "$MERMAID_JS_PATH" ]; then
        echo "Mermaid.js not found. Downloading version ${MERMAID_JS_VERSION}..."
        curl -fL "https://cdn.jsdelivr.net/npm/mermaid@${MERMAID_JS_VERSION}/dist/mermaid.min.js" -o "$MERMAID_JS_PATH"
    else
        echo "Mermaid.js version ${MERMAID_JS_VERSION} already exists at ${MERMAID_JS_PATH}."
    fi
fi


echo "Installation and update complete!"
