#!/usr/bin/env sh
set -eu

REPO_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
BUILD_DIR="${TMPDIR:-/tmp}/infrai-balance-guardian-classes"
mkdir -p "$BUILD_DIR"
find "$REPO_DIR/src/main/java" "$REPO_DIR/src/test/java" -name '*.java' -print | sort > "$BUILD_DIR/sources.txt"
javac -d "$BUILD_DIR" @"$BUILD_DIR/sources.txt"
java -cp "$BUILD_DIR" com.balanceguard.BalanceProtectionDecisionTest
