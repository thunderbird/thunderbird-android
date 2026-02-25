#!/bin/bash
# Test Python scripts and dependencies in a temporary environment
# Usage: ./scripts/test_python_scripts.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "Python Scripts Test"
echo "==================="
echo ""

# Check if Python is available
if ! command -v python3 &> /dev/null; then
    echo "✗ Error: python3 not found"
    exit 1
fi

echo "✓ Python: $(python3 --version)"

# Create temporary virtual environment
VENV_DIR=$(mktemp -d)
python3 -m venv "$VENV_DIR" > /dev/null 2>&1
source "$VENV_DIR/bin/activate"

# Install dependencies
echo "Installing dependencies..."
pip install --quiet --upgrade pip
pip install --quiet -r "$SCRIPT_DIR/requirements.txt"
echo "✓ Dependencies installed"
echo ""

# Test scripts compile
echo "Testing script compile..."
echo ""
python3 -m py_compile "$SCRIPT_DIR/ci/render-notes.py" && echo "  ✓ render-notes.py"
python3 -m py_compile "$SCRIPT_DIR/ci/setup_release_automation" && echo "  ✓ setup_release_automation"
python3 -m py_compile "$SCRIPT_DIR/ci/merges/merge_gradle.py" && echo "  ✓ merge_gradle.py"

TEST_RESULT=$?
echo ""

# Clean up
deactivate
rm -rf "$VENV_DIR"

if [ $TEST_RESULT -eq 0 ]; then
    echo "✓ All tests passed!"
    exit 0
else
    echo "✗ Some tests failed"
    exit 1
fi
