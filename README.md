# Ubirch Accounting Service

This service listens for AcctEvent records and stores them on Cassandra. It exposes a http interface as well that support querying.

1. [Categories supported](#categories)
2. [Query through http Interface](#http-interface)
3. [Ingestion through Kafka Interface](#kafka-interface)

## Categories

The system has two principal categories. Anchoring and Verification.

All UPPs that pass by Niomon are registered against the Accounting Service by the Event Log.

All UPPs/Hashes that are verified with the version 2 of the verification service are registered against the Accounting Service

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
curl -s -X GET \
    -H "authorization: bearer ${token}" \
    -H "content-type: application/json" \
    "${host}/api/acct_events/v1/${ownerId}" | jq .
```

**Fields**

_ownerId_: it is the keycloak id of the logged-in user. 

_identity_id_: (Optional) It is a device id or identity id. 

#### Keycloak Token and Responses
 
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

# Kafka Interface

The system will be listening to the configured topic and will store the account events to cassandra. The expected data object that
is required is as it follows:

## An Accounting Event

```json
{
  "id":"d1b6f970-2f6b-4c94-aa49-a7bb5b3ba363",
  "ownerId":"6cb65b4e-4121-47cd-845a-63f4005fe6b3",
  "identityId":"39092dd9-0e72-41b3-b6b0-cd414e6d55a2",
  "category":"verification",
  "sub_category": "entry_a",
  "occurredAt":"2020-11-06T12:42:34.976Z"
}
```

**Fields**

_id_: it represents the id of the event.
 
_ownerId_: it is the keycloak id of the logged-in user.

_identityId_: It represents the identity that generated the UPP or event. The device id or app id.

_category_: It represents the kind of event. That's to say, what action originated it.

_occurredAt_: It represents the time at which the event took place. 
