#!/bin/bash

local=$1
remote_host="https://api.console.dev.ubirch.com"
host="http://localhost:8081"

if [ "$local" == "-r" ]
then
  host=$remote_host
fi

realm=ubirch-default-realm
token=CHANGEME

curl -s --location --request GET "$host/ubirch-web-ui/api/v1/tenants?realm=$realm&page=0&size=500" \
--header "Authorization: Bearer $token" | jq .
