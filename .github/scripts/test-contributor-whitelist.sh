#!/bin/bash
# Test script to validate the contributor whitelist functionality
# This simulates the whitelist loading logic used in the GitHub Actions workflow

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WHITELIST_FILE="${SCRIPT_DIR}/../contributor-whitelist.txt"

echo "Testing contributor whitelist functionality..."
echo "================================================"
echo ""

# Test 1: Whitelist file exists
echo "Test 1: Checking if whitelist file exists..."
if [ -f "$WHITELIST_FILE" ]; then
    echo "✓ Whitelist file exists at: $WHITELIST_FILE"
else
    echo "✗ Whitelist file not found at: $WHITELIST_FILE"
    exit 1
fi
echo ""

# Test 2: Whitelist file is readable
echo "Test 2: Checking if whitelist file is readable..."
if [ -r "$WHITELIST_FILE" ]; then
    echo "✓ Whitelist file is readable"
else
    echo "✗ Whitelist file is not readable"
    exit 1
fi
echo ""

# Test 3: Load and parse whitelist
echo "Test 3: Loading and parsing whitelist..."
whitelist=$(grep -v '^#' "$WHITELIST_FILE" | grep -v '^$' | sort)
count=$(echo "$whitelist" | wc -l)
echo "✓ Loaded $count contributor(s) from whitelist"
echo ""

# Test 4: Display loaded contributors
echo "Test 4: Displaying loaded contributors..."
echo "$whitelist" | while read -r username; do
    echo "  - $username"
done
echo ""

# Test 5: Check for duplicates
echo "Test 5: Checking for duplicate entries..."
duplicates=$(echo "$whitelist" | sort | uniq -d)
if [ -z "$duplicates" ]; then
    echo "✓ No duplicate entries found"
else
    echo "✗ Duplicate entries found:"
    echo "$duplicates"
    exit 1
fi
echo ""

# Test 6: Validate username format (basic check)
echo "Test 6: Validating username format..."
# GitHub usernames can be 1-39 characters, alphanumeric and hyphens
# Cannot start or end with hyphen, but single characters are allowed
invalid_usernames=$(echo "$whitelist" | grep -v -E '^[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?$' || true)
if [ -z "$invalid_usernames" ]; then
    echo "✓ All usernames have valid format"
else
    echo "⚠ Some usernames may have unusual format (this might be okay):"
    echo "$invalid_usernames"
fi
echo ""

# Test 7: Simulate whitelist check for known contributor
echo "Test 7: Simulating whitelist check for known contributor..."
test_user="kthoms"
if echo "$whitelist" | grep -q "^${test_user}$"; then
    echo "✓ Known contributor '$test_user' found in whitelist"
else
    echo "✗ Known contributor '$test_user' NOT found in whitelist"
    exit 1
fi
echo ""

# Test 8: Simulate whitelist check for unknown contributor
echo "Test 8: Simulating whitelist check for unknown contributor..."
test_user="unknownuser123"
if echo "$whitelist" | grep -q "^${test_user}$"; then
    echo "✗ Unknown contributor '$test_user' found in whitelist (should not be there)"
    exit 1
else
    echo "✓ Unknown contributor '$test_user' correctly not in whitelist"
fi
echo ""

echo "================================================"
echo "All tests passed! ✓"
echo ""
echo "Summary:"
echo "  - Whitelist file: $WHITELIST_FILE"
echo "  - Contributors: $count"
