#!/bin/sh
set -eu

REPO="${GITIA_REPO:-julianmacagno/Gitnuro}"
URL="https://github.com/$REPO/releases/latest/download/GitIA-universal.jar"

if ! command -v java >/dev/null 2>&1; then
    echo "Java 21 or newer is required. Ubuntu: sudo apt install openjdk-21-jre | macOS: brew install openjdk@21" >&2
    exit 1
fi

major=$(java -version 2>&1 | sed -n '1s/[^"]*"\([0-9]*\).*/\1/p')
if [ "${major:-0}" -lt 21 ]; then
    echo "Java 21 or newer is required, found $major" >&2
    exit 1
fi

tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT

curl -fsSL "$URL" -o "$tmp/GitIA.jar"
java -jar "$tmp/GitIA.jar" --install
