#!/bin/bash -e

curl "$ELASTIC_HOST/_cat/indices"

# Since ontology-generator v5, content files sit under elastic/content/ rather
# than directly under elastic/. Fall back to the legacy flat layout if needed.
if [ -d ./elastic/content ]; then
  CONTENT_DIR=./elastic/content
else
  CONTENT_DIR=./elastic
fi

# Grab existing ids from both index file types to use later
onto_example_id=$(grep '"_id":' "$CONTENT_DIR"/onto_es__ontology_* | awk -F'"_id": "' '{print $2}' | awk -F'"' '{print $1}' | head -n 1)
cc_example_id=$(grep '"_id":' "$CONTENT_DIR"/onto_es__codeable_concept_* | awk -F'"_id": "' '{print $2}' | awk -F'"' '{print $1}' | head -n 1)


# Check if the elastic search server correctly delivers the ontology document
response=$(curl -s -w "%{http_code}" -o response_body "$ELASTIC_HOST/ontology/_doc/$onto_example_id")
http_code="${response: -3}"
json_body=$(cat response_body)

if [ "$http_code" -eq 200 ]; then
    if echo "$json_body" | jq '.found == true' | grep -q true; then
        echo "Ontology Document with id $onto_example_id found in elastic search"
    else
        echo "Empty or nonexistent response from elastic search for ontology document with id $onto_example_id"
        exit 1
    fi
else
    echo "Response code $http_code"
    exit 1
fi

# Check if the elastic search server correctly delivers the codeable concept document
response=$(curl -s -w "%{http_code}" -o response_body "$ELASTIC_HOST/codeable_concept/_doc/$cc_example_id")
http_code="${response: -3}"
json_body=$(cat response_body)

if [ "$http_code" -eq 200 ]; then
    if echo "$json_body" | jq '.found == true' | grep -q true; then
        echo "Codeable Concept Document with id $cc_example_id found in elastic search"
    else
        echo "Empty or nonexistent response from elastic search for codeable_concept document with id $cc_example_id"
        exit 1
    fi
else
    echo "Response code $http_code"
    exit 1
fi

access_token="$(curl -s --request POST \
  --url http://localhost:8083/auth/realms/dataportal/protocol/openid-connect/token \
  --header 'Content-Type: application/x-www-form-urlencoded' \
  --data grant_type=password \
  --data client_id=dataportal-webapp \
  --data username=testuser \
  --data password=testpassword \
  --data scope=openid | jq '.access_token' | tr -d '"')"


# Check if the terminology search is working correctly (curl->backend->elastic)
response=$(curl -s -w "%{http_code}" --header "Authorization: Bearer $access_token" -o response_body "http://localhost:8091/api/v5/terminology/entry/search?searchterm=Blutdruck")
http_code="${response: -3}"
json_body=$(cat response_body)

if [ "$http_code" -eq 200 ]; then
    if echo "$json_body" | jq '.totalHits > 0' | grep -q true; then
        echo "OK response with non-empty array on onto search with searchterm"
    else
        echo "Empty or nonexistent response on onto search with searchterm"
        exit 1
    fi
else
    echo "Response code $http_code"
    exit 1
fi


# Check if the ontology document retrieval is working correctly (curl->backend->elastic)
response=$(curl -s -w "%{http_code}" --header "Authorization: Bearer $access_token" -o response_body "http://localhost:8091/api/v5/terminology/entry/$onto_example_id")
http_code="${response: -3}"
json_body=$(cat response_body)

if [ "$http_code" -eq 200 ]; then
    if echo "$json_body" | jq -e '.termcode and .termcode != ""' | grep -q true; then
        echo "OK response with non-empty array on get onto by id"
    else
        echo "Empty or nonexistent response on get onto by id"
        exit 1
    fi
else
    echo "Response code $http_code"
    exit 1
fi


# Check if the codeable concept search is working correctly (curl->backend->elastic)
response=$(curl -s -w "%{http_code}" --header "Authorization: Bearer $access_token" -o response_body "http://localhost:8091/api/v5/codeable-concept/entry/search?searchterm=Vectorcardiogram")
http_code="${response: -3}"
json_body=$(cat response_body)

if [ "$http_code" -eq 200 ]; then
    if echo "$json_body" | jq '.totalHits > 0' | grep -q true; then
        echo "OK response with non-empty array on cc search with searchterm"
    else
        echo "Empty or nonexistent response on cc search with searchterm"
        exit 1
    fi
else
    echo "Response code $http_code"
    exit 1
fi


# Check if the codeable concept document retrieval is working correctly (curl->backend->elastic)
response=$(curl -s -w "%{http_code}" --header "Authorization: Bearer $access_token" -o response_body "http://localhost:8091/api/v5/codeable-concept/entry?ids=$cc_example_id")
http_code="${response: -3}"
json_body=$(cat response_body)

if [ "$http_code" -eq 200 ]; then
    if echo "$json_body" | jq -e '.[0].termCode.code and .[0].termCode.code != ""' | grep -q true; then
        echo "OK response with non-empty array on get cc by id"
    else
        echo "Empty or nonexistent response on get cc by id"
        exit 1
    fi
else
    echo "Response code $http_code"
    exit 1
fi


# Check if searching for an exact code returns the same amount of results whether a filter (for the correct terminology)
# is set or not. In this case, the LOINC code 104683-8 is used
response=$(curl -s -w "%{http_code}" --header "Authorization: Bearer $access_token" -o response_body "http://localhost:8091/api/v5/terminology/entry/search?searchterm=104683-8&availability=&contexts=&kds-modules=&terminologies=&page-size=50")
results_without_filter=$(cat response_body | jq -e '.totalHits')

response=$(curl -s -w "%{http_code}" --header "Authorization: Bearer $access_token" -o response_body "http://localhost:8091/api/v5/terminology/entry/search?searchterm=104683-8&availability=&contexts=&kds-modules=&terminologies=http://loinc.org&page-size=50")
results_with_filter=$(cat response_body | jq -e '.totalHits')

if [ "$results_without_filter" -ge "$results_with_filter" ]; then
    echo "Filtered results are NOT larger than unfiltered results. OK."
else
    echo "Filtered results are larger than unfiltered results. NOK."
    exit 1
fi

echo "All elastic search tests completed without errors"
exit 0
