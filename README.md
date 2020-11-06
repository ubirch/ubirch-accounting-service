# Ubirch Accounting Service

This service listens for AcctEvent records and stores them on Cassandra.

## An Account Event

```json
{
  "id":"d1b6f970-2f6b-4c94-aa49-a7bb5b3ba363",
  "ownerId":"6cb65b4e-4121-47cd-845a-63f4005fe6b3",
  "identityId":"39092dd9-0e72-41b3-b6b0-cd414e6d55a2",
  "category":"verification",
  "description":"Lana de rey concert",
  "occurredAt":"2020-11-06T12:42:34.976Z"
}
```
