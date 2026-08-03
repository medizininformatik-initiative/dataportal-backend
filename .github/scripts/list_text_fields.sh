#!/usr/bin/env bash
# List all searchable "text" fields (excludes keyword, and text fields with
# "index": false, which ES stores but cannot search) for one or more
# Elasticsearch indices.
#
# Usage:
#   ./list_text_fields.sh                          # runs against default indices below
#   ./list_text_fields.sh <index_name> [es_url]     # single index
set -euo pipefail

ES_URL_DEFAULT="http://localhost:9200"
DEFAULT_INDICES=(ontology profile codeable_concept)

JQ_FILTER='
  def walk(path; node):
    node | to_entries[] |
    .key as $k | .value as $v |
    (if path == "" then $k else path + "." + $k end) as $p |
    (
      (if ($v.type // "") == "text" and ($v.index != false)
        then $p else empty end),
      (if ($v.fields // {}) != {}
        then ($v.fields | to_entries[]
              | select(.value.type == "text" and (.value.index != false))
              | $p + "." + .key)
        else empty end),
      (if ($v.properties // null) != null then walk($p; $v.properties) else empty end)
    );

  .[].mappings.properties as $props | walk(""; $props)
'

list_text_fields() {
  local index="$1"
  local es_url="$2"
  curl -s "${es_url}/${index}/_mapping" | jq -r "$JQ_FILTER" | sort
}

if [[ $# -ge 1 ]]; then
  list_text_fields "$1" "${2:-$ES_URL_DEFAULT}"
else
  for idx in "${DEFAULT_INDICES[@]}"; do
    echo "=== ${idx} ==="
    list_text_fields "$idx" "$ES_URL_DEFAULT"
    echo
  done
fi
