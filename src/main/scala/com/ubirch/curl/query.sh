#!/bin/bash

local=$1
remote_host="https://accounting.dev.ubirch.com"
host="http://localhost:8081"

if [ "$local" == "-r" ]
then
  host=$remote_host
fi

echo "=> host: $host"

identity_id=fce88cda-0311-49a7-8bab-745dbd0f3c7e
category=verification
date=2022-02-03
hour=0
sub_category=entry-b
mode=count

token=eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJodHRwczovL3Rva2VuLmRldi51YmlyY2guY29tIiwic3ViIjoiOTYzOTk1ZWQtY2UxMi00ZWE1LTg5ZGMtYjE4MTcwMWQxZDdiIiwiYXVkIjoiaHR0cHM6Ly9hcGkuY29uc29sZS5kZXYudWJpcmNoLmNvbSIsImlhdCI6MTY0Mzg3NTk4NSwianRpIjoiYzA2ODA5NjItOWM4Ni00NTA0LWI3YzMtMjBlMGNkNmE0ZGIyIiwic2NwIjpbInRoaW5nOmdldGluZm8iXSwicHVyIjoiQWNjb3VudGluZyBTZXJ2aWNlIiwidGdwIjpbXSwidGlkIjpbImZjZTg4Y2RhLTAzMTEtNDlhNy04YmFiLTc0NWRiZDBmM2M3ZSJdLCJvcmQiOltdfQ.FhoNUxmkvDmAuzfXv1mTnjZOvxe6Z9Kuz4CtSC4T8VeIKd4iBmz9GPc0M23_TAio0ScyEtOd4IkEYVnqYuCNcQ

curl -v -X GET -H "authorization: bearer $token" \
 -H "content-type: application/json" \
  "http://localhost:8081/api/acct_events/v1/$identity_id?cat=$category&date=$date&hour=$hour&sub_cat=$sub_category&mod=$mode" \

