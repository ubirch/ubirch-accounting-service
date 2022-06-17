#!/bin/bash

local=$1
remote_host="https://accounting.dev.ubirch.com"
host="http://localhost:8090"

if [ "$local" == "-r" ]
then
  host=$remote_host
fi

echo "=> host: $host"

tenant_id=29b05321-7a34-4bbb-80f4-2bd4ee91033c
invoice_id=12539f76-c7e9-47d6-b37b-4b59380721ac
invoice_date=2022-06-04
order_ref=ubirch
category=verification
from=2022-06-01
to=2022-06-17

token=CHANGEME

curl -v -X GET -H "authorization: bearer $token" \
 -H "content-type: application/json" \
  "$host/api/acct_events/v1?tenant_id=$tenant_id&invoice_id=$invoice_id&invoice_date=$invoice_date&order_ref=$order_ref&cat=$category&from=$from&to=$to"
