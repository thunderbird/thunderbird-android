#!/bin/bash

function fail() {
  echo "Error: $*"
  exit 1
}

# Check if tools are installed
command -v docker &> /dev/null || fail "Docker is not installed"

# Default values
debug=false

# Parse command-line arguments
for arg in "$@"; do
  case $arg in
    --debug)
      debug=true
      shift
      ;;
    *)
      fail "Unknown argument: $arg"
      ;;
  esac
done

if [ "$debug" = true ]; then
  docker run --rm -v "$(pwd)":/repo -it fluidattacks/cli:latest /bin/bash
  exit
fi
docker run --rm -v "$(pwd)":/repo fluidattacks/cli:latest skims scan /repo/config/fluidattacks/config.yaml
