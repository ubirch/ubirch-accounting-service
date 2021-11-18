# Ubirch Accounting Service

This service listens for AcctEvent records and stores them on Cassandra. It exposes a http interface as well that support querying.

## Http Interface

1. [Getting Started](#steps-to-prepare-a-request)
2. [List Your Acct Events](#list-your-acct-events)
3. [Keycloak and Responses](#keycloak-token-and-responses)

### Steps to prepare a request

1. Get your keycloak token.
2. Prepare the query params.
3. Prepare the request and send.

### List Your Acct Events

#### Keycloak Token

```json
token=`curl -s -d "client_id=ubirch-2.0-user-access" -d "username=$TOKEN_USER" -d "password=$TOKEN_PASS" -d "grant_type=password" -d "client_secret=$TOKEN_CLIENT_ID" $keycloak | jq -r .access_token`
```

#### Get Request

```shell script
curl -v -X GET -H "authorization: bearer $token" \
 -H "content-type: application/json" \
  "http://localhost:8081/api/acct_events/v1/$owner?cat=$category&identity_id=$device&start=$start&end=$end&only_count=$count&bucketed=$bucketed" \
 | jq . 
```

**Fields**

_ownerId_: it is the keycloak id of the logged-in user. 

_identity_id_: (Optional)* It is a device id or identity id. 

_category_: (Optional)* It is the category for the stored event.

_start_: (Optional) It is the start time of the query. The format is "yyyy-M-dd"

_end_: (Optional) It is the end time of the query. The format is "yyyy-M-dd"

_only_count_: (Optional)* It turns on the count mode only. It will count the events only.

_bucketed_: (Optional)* It will count aggregating by day.

> The following image shows the possible combinations of the principal queries.

![Accounting Params](accouting_params.png)

### Keycloak Token and Responses
 
In order for any request be received and executed, the initiator must provide proof it has been granted with the required permissions. 
In order to do so, its request must contain an Authorization header. 

#### The Header

```
Authorization: <type> <token>

where 
  <type> is Bearer
  <token> is the JWT token for the current logged in user. This token originates from Keycloak.
``` 
  
#### The Responses

```
The <response> codes could be:

1. <200 OK>           When the system found a proper verification.
2. <400 Badrequest>   When the incoming data has not been properly parsed or accepted.            
3. <403 Forbidden>    When the token is invalid.
4. <401 Unauthorized> When no Authorization header is found in the request.
                      In this case, the response will contain the following header 
                      WWW-Authenticate: <type> realm=<realm>
                      
                      where <type> is Bearer and
                           <realm> is "Ubirch Token Service"
5. <500 Internal Server Error> When an internal error happened from which it is not possible to recover.
```

### Swagger

Visit https://accounting.dev.ubirch.com/docs on your browser to see the swagger docs.

# Kafka

The system will be listening to the configured topic and will store the account events to cassandra. The expected data object that
is required is as it follows:

## An Accounting Event

```json
{
  "id":"d1b6f970-2f6b-4c94-aa49-a7bb5b3ba363",
  "ownerId":"6cb65b4e-4121-47cd-845a-63f4005fe6b3",
  "identityId":"39092dd9-0e72-41b3-b6b0-cd414e6d55a2",
  "category":"verification",
  "description":"Lana de rey concert",
  "token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJodHRwczovL3Rva2VuLmRldi51YmlyY2guY29tIiwic3ViIjoiOTYzOTk1ZWQtY2UxMi00ZWE1LTg5ZGMtYjE4MTcwMWQxZDdiIiwiYXVkIjoiaHR0cHM6Ly92ZXJpZnkuZGV2LnViaXJjaC5jb20iLCJleHAiOjc5MTgxMTgxMDcsImlhdCI6MTYwNjcyNzcwNywianRpIjoiNDFiMDFkNzMtYTdkZi00N2ZhLWFkMDAtNzEwMWJjZTBmZmVhIiwicHVycG9zZSI6IlhtYXMgQWR2ZW50cyIsInRhcmdldF9pZGVudGl0aWVzIjpbIjc1NDlhY2Q4LTkxZTEtNDIzMC04MzNhLTJmMzg2ZTA5Yjk2ZiJdLCJyb2xlIjoidmVyaWZpZXIifQ.bRV1DmKwFZXdB5XD99xxEA8MhcBuE9N5UkThuyIajw4VvECvsq6PHShjReSmhcX_fqK-Bs-FioOC0Eh0odrYzQ",
  "occurredAt":"2020-11-06T12:42:34.976Z"
}
```

**Fields**

_id_: it represents the id of the event.
 
_ownerId_: it is the keycloak id of the logged-in user.

_identityId_: It represents the identity that generated the UPP. The device id or app id.

_category_: It represents the kind of event. That's to say, what action originated it.

_description_: It is a brief description for what this event accounts for.

_token_: It represents the possible token that might have been used to generate the action.

_occurredAt_: It represents the time at which the event took place. 
