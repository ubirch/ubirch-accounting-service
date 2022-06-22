#!/bin/bash

local=$1
remote_host="https://api.console.dev.ubirch.com"
host="http://localhost:8081"

if [ "$local" == "-r" ]
then
  host=$remote_host
fi

tenant_id=90ce9ca6-cefb-4b25-a1f0-acf9fa7a35fd
realm=ubirch-default-realm
token=CHANGEME

curl -v --location --request GET "$host/ubirch-web-ui/api/v1/tenants/$tenant_id/devices?realm=$realm&page=0&size=10" \
--header "Authorization: Bearer $token" \
  | jq .

