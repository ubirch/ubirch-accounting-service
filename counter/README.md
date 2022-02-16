# Account Counter Tool

It is a node-based application that allows to query the accounting service API.

- Make sure you have node installed. [Node page](https://nodejs.org/en/).
- Make sure you have npm installed. [NPM js page](https://docs.npmjs.com/downloading-and-installing-node-js-and-npm)
- Run 'npm install'
- Create a "config.json" file in the root folder with the following info.
    ```json
    {
    "host": "http://localhost:8081",
    "OR": "https://accounting.dev.ubirch.com",
    "token": "UBIRCH JWT TOKEN with get:info scope and the target id as the identity id"
    }
    ```
- `monthlyReport.js --id=UUID_OF_IDENTITY --cat=CATEGORY --date=YYYY-MM-DD --subcat=SUBCATEGORY` -> subcat is optional

![Result Query](result_query.png)
