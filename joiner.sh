#!/bin/bash
# joiner.sh — Merge GraalVM native-image metadata config files by type
#
# Finds all *-config.json files under a search directory (default: target),
# groups them by filename, and merges each group into a single file using
# jsont with the appropriate JS merge template.
#
# Usage:
#   ./joiner.sh [search_dir] [output_dir]
#
# Examples:
#   ./joiner.sh                                     # defaults: target → target/merged-native-config
#   ./joiner.sh target/graalvm-reachability-metadata merged-output
#
# Environment:
#   JSONT  — path to the jsont binary (default: target/jsont)
#            e.g. JSONT="./src/main/java/JsonT.java" ./joiner.sh   # use jbang

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SEARCH_DIR="${1:-target}"
OUTPUT_DIR="${2:-target/merged-native-config}"
JSONT="${JSONT:-target/jsont}"

# ── Templates ────────────────────────────────────────────────────────────────
MERGE_ARRAYS="${SCRIPT_DIR}/merge-arrays.js"
MERGE_OBJECTS="${SCRIPT_DIR}/merge-objects.js"
MERGE_SERIALIZATION="${SCRIPT_DIR}/merge-serialization.js"

# ── Helpers ──────────────────────────────────────────────────────────────────

die() { echo "ERROR: $*" >&2; exit 1; }

check_prereqs() {
    if [ ! -x "$JSONT" ] && [ ! -f "$JSONT" ]; then
        die "jsont not found at '$JSONT'. Build with 'mvn -Pnative install' or set JSONT env var."
    fi
    for tmpl in "$MERGE_ARRAYS" "$MERGE_OBJECTS" "$MERGE_SERIALIZATION"; do
        [ -f "$tmpl" ] || die "Template not found: $tmpl"
    done
    [ -d "$SEARCH_DIR" ] || die "Search directory not found: $SEARCH_DIR"
}

# Discover all unique *-config.json basenames under the search directory
find_config_names() {
    find "$SEARCH_DIR" -name '*-config.json' -type f \
        | xargs -I{} basename {} \
        | sort -u
}

# Count how many files of a given config name exist
count_files() {
    local config_name="$1"
    find "$SEARCH_DIR" -name "$config_name" -type f | wc -l
}

# Assemble all files of a given config name into a single JSON array:
#   [ <contents-of-file1>, <contents-of-file2>, ... ]
# This feeds into jsont as the input JSON.
assemble_json_array() {
    local config_name="$1"
    local first=true

    printf '[\n'
    while IFS= read -r f; do
        # skip empty files or files that are just whitespace
        if [ ! -s "$f" ]; then
            continue
        fi
        if $first; then
            first=false
        else
            printf ',\n'
        fi
        cat "$f"
    done < <(find "$SEARCH_DIR" -name "$config_name" -type f | sort)
    printf '\n]\n'
}

# Pick the right JS merge template based on config filename
template_for() {
    local config_name="$1"
    case "$config_name" in
        resource-config.json)
            echo "$MERGE_OBJECTS"
            ;;
        serialization-config.json)
            echo "$MERGE_SERIALIZATION"
            ;;
        # reflect-config.json, jni-config.json, proxy-config.json, and any
        # other array-based config all use the flat array concatenation template
        *)
            echo "$MERGE_ARRAYS"
            ;;
    esac
}

# ── Main ─────────────────────────────────────────────────────────────────────

check_prereqs

mkdir -p "$OUTPUT_DIR"

echo "──────────────────────────────────────────────"
echo "  joiner.sh — GraalVM native-image config merger"
echo "  search : $SEARCH_DIR"
echo "  output : $OUTPUT_DIR"
echo "  jsont  : $JSONT"
echo "──────────────────────────────────────────────"
echo

CONFIG_NAMES="$(find_config_names)"

if [ -z "$CONFIG_NAMES" ]; then
    echo "No *-config.json files found under $SEARCH_DIR"
    exit 0
fi

for config_name in $CONFIG_NAMES; do
    n=$(count_files "$config_name")
    template="$(template_for "$config_name")"
    template_base="$(basename "$template")"
    output_file="$OUTPUT_DIR/$config_name"

    printf "%-30s  %3d file(s)  ← %s\n" "$config_name" "$n" "$template_base"

    assemble_json_array "$config_name" \
        | "$JSONT" "$template" - "$output_file" 2>/dev/null

    if [ -f "$output_file" ]; then
        size=$(wc -c < "$output_file")
        printf "%-30s  wrote %s bytes → %s\n" "" "$size" "$output_file"
    else
        echo "  WARNING: output not created for $config_name" >&2
    fi
done

echo
echo "Done. Merged configs written to $OUTPUT_DIR/"
echo
echo "To install as the project native-image metadata, copy into:"
echo "  src/main/resources/META-INF/native-image/<groupId>/<artifactId>/"
echo
echo "Or for a quick install into /usr/local/bin:"
echo "  sudo cp target/jsont /usr/local/bin/jsont"
