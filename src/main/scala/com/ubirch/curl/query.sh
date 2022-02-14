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

token=eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJodHRwczovL3Rva2VuLmRldi51YmlyY2guY29tIiwic3ViIjoiOTYzOTk1ZWQtY2UxMi00ZWE1LTg5ZGMtYjE4MTcwMWQxZDdiIiwiYXVkIjoiaHR0cHM6Ly9hcGkuY29uc29sZS5kZXYudWJpcmNoLmNvbSIsImlhdCI6MTY0Mzg3NTk4NSwianRpIjoiYzA2ODA5NjItOWM4Ni00NTA0LWI3YzMtMjBlMGNkNmE0ZGIyIiwic2NwIjpbInRoaW5nOmdldGluZm8iXSwicHVyIjoiQWNjb3VudGluZyBTZXJ2aWNlIiwidGdwIjpbXSwidGlkIjpbImZjZTg4Y2RhLTAzMTEtNDlhNy04YmFiLTc0NWRiZDBmM2M3ZSJdLCJvcmQiOltdfQ.FhoNUxmkvDmAuzfXv1mTnjZOvxe6Z9Kuz4CtSC4T8VeIKd4iBmz9GPc0M23_TAio0ScyEtOd4IkEYVnqYuCNcQ

curl -s -X GET -H "authorization: bearer $token" \
 -H "content-type: application/json" \
  "http://localhost:8081/api/acct_events/v1/$identity_id?cat=$category&date=$date&sub_cat=$sub_category" \
  | jq .

