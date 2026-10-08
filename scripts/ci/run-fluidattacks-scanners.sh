#!/bin/bash

function fail() {
  echo "Error: $*"
  exit 1
}

# Check if tools are installed
command -v docker &> /dev/null || fail "Docker is not installed"

# Default values
debug=false

IMAGE_SAST="fluidattacks/sast:latest"
IMAGE_SCA="fluidattacks/sca:latest"

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
  docker run --rm -v "$(pwd)":/repo -it "$IMAGE_SAST" /bin/bash
  docker run --rm -v "$(pwd)":/repo -it "$IMAGE_SCA" /bin/bash
  exit
fi

docker run --rm -v "$(pwd)":/repo "$IMAGE_SAST" \
  sast scan /repo/config/fluidattacks/config-sast.yaml
docker run --rm -v "$(pwd)":/repo "$IMAGE_SCA" \
  sca scan /repo/config/fluidattacks/config-sca.yaml
