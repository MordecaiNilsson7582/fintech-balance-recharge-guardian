#!/usr/bin/env sh
set -eu

REPO_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
BUILD_DIR="${TMPDIR:-/tmp}/infrai-balance-guardian-classes"
mkdir -p "$BUILD_DIR"
find "$REPO_DIR/src/main/java" -name '*.java' -print | sort > "$BUILD_DIR/main-sources.txt"
javac -d "$BUILD_DIR" @"$BUILD_DIR/main-sources.txt"
java -cp "$BUILD_DIR" com.balanceguard.BalanceGuardian "$@"
