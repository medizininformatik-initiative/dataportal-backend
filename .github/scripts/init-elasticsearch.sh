#!/bin/bash -e

ABSOLUTE_FILEPATH="${ELASTIC_FILEPATH//TAGPLACEHOLDER/$ONTOLOGY_GIT_TAG}$ELASTIC_FILENAME"
echo "Downloading $ABSOLUTE_FILEPATH"
response_onto_dl=$(curl --write-out "%{http_code}" -sLO "$ABSOLUTE_FILEPATH")

if [ "$response_onto_dl" -ne 200 ]; then
  echo "Could not download ontology file. Maybe the tag $ONTOLOGY_GIT_TAG does not exist? Error code was $response_onto_dl"
  exit 1
fi

unzip -o "$ELASTIC_FILENAME"

EXTRACT_DIR=elastic

# Since ontology-generator v5, archives are laid out with index/content/pipeline
# subdirectories. Older archives are flat: index definition files (named
# *_index.json) and content files sit side by side in $EXTRACT_DIR, and there
# are no pipelines. Detect which layout we got and set the dirs accordingly.
if [ -d "$EXTRACT_DIR/index" ]; then
  echo "Detected archive layout with index/content/pipeline subdirectories."
  INDEX_DIR="$EXTRACT_DIR/index"
  CONTENT_DIR="$EXTRACT_DIR/content"
  PIPELINE_DIR="$EXTRACT_DIR/pipeline"
else
  echo "Detected legacy flat archive layout. Using $EXTRACT_DIR directly; no pipelines in this layout."
  INDEX_DIR="$EXTRACT_DIR"
  CONTENT_DIR="$EXTRACT_DIR"
  PIPELINE_DIR=""
fi

echo "(Trying to) delete existing indices"
for FILE in "$INDEX_DIR"/*_index.json; do
  [[ -f "$FILE" ]] || continue
  INDEX_NAME=$(basename "$FILE" .json)
  INDEX_NAME="${INDEX_NAME%_index}"
  echo "Deleting $INDEX_NAME index"
  curl --request DELETE "$ELASTIC_HOST/$INDEX_NAME"
done

if [ -n "$PIPELINE_DIR" ] && [ -d "$PIPELINE_DIR" ]; then
  echo "Creating pipelines..."
  for FILE in "$PIPELINE_DIR"/*.json; do
    [[ -f "$FILE" ]] || continue
    PIPELINE_NAME=$(basename "$FILE" .json)
    echo "Creating pipeline $PIPELINE_NAME..."
    curl --write-out "%{http_code}\n" -s --output /dev/null -XPUT -H 'Content-Type: application/json' "$ELASTIC_HOST/_ingest/pipeline/$PIPELINE_NAME" -d @"$FILE"
  done
fi

declare -A INDEX_RESPONSES

for FILE in "$INDEX_DIR"/*_index.json; do
  [[ -f "$FILE" ]] || continue
  INDEX_NAME=$(basename "$FILE" .json)
  INDEX_NAME="${INDEX_NAME%_index}"
  echo "Creating $INDEX_NAME index..."
  response_index=$(curl --write-out "%{http_code}" -s --output /dev/null -XPUT -H 'Content-Type: application/json' "$ELASTIC_HOST/$INDEX_NAME" -d @"$FILE")
  INDEX_RESPONSES[$INDEX_NAME]=$response_index
done
echo "Done."

for FILE in "$CONTENT_DIR"/*; do
  [[ -f "$FILE" ]] || continue
  BASENAME=$(basename "$FILE")

  # Only process JSON/NDJSON content files (named onto_es__<index>[_<n>].json/.ndjson)
  if [[ "$BASENAME" == onto_es__*.json || "$BASENAME" == onto_es__*.ndjson ]]; then
    # Derive the target index name: strip the prefix, the extension, and any
    # trailing numeric chunk suffixes (e.g. _1, _1_0, _9_38)
    INDEX_NAME="${BASENAME#onto_es__}"
    INDEX_NAME="${INDEX_NAME%.*}"
    while [[ "$INDEX_NAME" =~ ^(.+)_[0-9]+$ ]]; do
      INDEX_NAME="${BASH_REMATCH[1]}"
    done

    if [[ "${INDEX_RESPONSES[$INDEX_NAME]}" -eq 200 || "$OVERRIDE_EXISTING" = "true" ]]; then
      echo "Uploading $BASENAME"
      curl -s --output /dev/null -XPOST -H 'Content-Type: application/json' --data-binary @"$FILE" "$ELASTIC_HOST/$INDEX_NAME/_bulk"
    else
      echo "Skipping $BASENAME because index was already existing. Set OVERRIDE_EXISTING to true to force creating a new index"
    fi
  fi
done

echo "All done"
