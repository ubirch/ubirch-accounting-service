# Account Counter

It is a node-based application that allows to query the accounting service API.

Create a "config.json" file in the root folder with the following info.

```json
{
"host": "http://localhost:8081",
"OR": "https://accounting.dev.ubirch.com",
"token": "UBIRCH JWT TOKEN with get:info scope and the target id as the identity id"
}
```

`node monthlyReport.js`

