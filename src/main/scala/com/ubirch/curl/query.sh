#!/bin/bash

local=$1
remote_host="https://accounting.dev.ubirch.com"
host="http://localhost:8081"

if [ "$local" == "-r" ]
then
  host=$remote_host
fi

echo "=> host: $host"

identity_id=12539f76-c7e9-47d6-b37b-4b59380721ac
category=verification
date=2022-02-04
sub_category=entry-a

token=CHANGEME

curl -s -X GET -H "authorization: bearer $token" \
 -H "content-type: application/json" \
  "http://localhost:8081/api/acct_events/v1/$identity_id?cat=$category&date=$date&sub_cat=$sub_category" \
  | jq .

