#!/bin/bash

local=$1
remote_host="https://accounting.dev.ubirch.com"
host="http://localhost:8081"

if [ "$local" == "-r" ]; then
  host=$remote_host
fi

echo "=> host: $host"

token=CHANGEME

curl -v -X POST http://localhost:8081/api/acct_events/v1/record \
  -H "authorization: bearer $token" \
  -H "content-type: application/json" \
  -d '[
        {
          "id":"d1b6f970-2f6b-4c94-aa49-a7bb5b3ba363",
          "ownerId":"6cb65b4e-4121-47cd-845a-63f4005fe6b3",
          "identityId":"39092dd9-0e72-41b3-b6b0-cd414e6d55a2",
          "category":"verification",
          "sub_category":"entry_a",
          "occurredAt":"2020-11-06T12:42:34.976Z"
        },
        {
          "id":"d1b6f970-2f6b-4c94-aa49-a7bb5b3ba364",
          "ownerId":"6cb65b4e-4121-47cd-845a-63f4005fe6b3",
          "identityId":"39092dd9-0e72-41b3-b6b0-cd414e6d55a2",
          "category":"verification",
          "sub_category":"entry_a",
          "occurredAt":"2020-11-06T12:42:34.976Z"
        }
      ]'
