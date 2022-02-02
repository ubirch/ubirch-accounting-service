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
identity_id=32c07ed9-c107-4e75-b28f-a750b15d2196
category=verification
date=2022-02-02
hour=22
mode=count

curl -s -X GET -H "authorization: bearer $token" \
 -H "content-type: application/json" \
  "http://localhost:8081/api/acct_events/v1/$owner?cat=$category&identity_id=$identity_id&date=$date&hour=$hour&mode=$mode" \
 | jq '.'
