#!/bin/bash

local=$1
remote_host="https://accounting.dev.ubirch.com"
host="http://localhost:8081"
keycloak="https://id.dev.ubirch.com/auth/realms/ubirch-default-realm/protocol/openid-connect/token"

if [ "$local" == "-r" ]
then
  host=$remote_host
fi

token=`curl -s -d "client_id=ubirch-2.0-user-access" -d "username=$TOKEN_USER" -d "password=$TOKEN_PASS" -d "grant_type=password" -d "client_secret=$TOKEN_CLIENT_ID" $keycloak | jq -r .access_token`

echo "=> host: $host"

owner=d63ecc03-f5a7-4d43-91d0-a30d034d8da3
device=03ebd518-8b09-45ec-a039-604fc8a9e687
category=verification
start=2020-01-01
end=2021-11-24
count=false
bucketed=true

curl -s -X GET -H "authorization: bearer $token" \
 -H "content-type: application/json" \
  "http://localhost:8081/api/acct_events/v1/$owner?cat=$category&identity_id=$device&start=$start&end=$end&only_count=$count&bucketed=$bucketed" \
 | jq .
