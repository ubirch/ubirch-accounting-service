#!/bin/bash

local=$1
remote_host="https://accounting.dev.ubirch.com"
host="http://localhost:8081"

if [ "$local" == "-r" ]; then
  host=$remote_host
fi

echo "=> host: $host"

token=CHANGEME

owner_id=963995ed-ce12-4ea5-89dc-b181701d1d7b

curl -s -X GET -H "authorization: bearer $token" \
 -H "content-type: application/json" \
  "http://localhost:8081/api/acct_events/v1/$owner_id/identities" \
  | jq '.'
