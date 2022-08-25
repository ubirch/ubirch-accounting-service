## General System

This image presents a high-level view of the accounting services and its interfaces.

![General System](assets/acct_events_v3_II.svg)

###  Components

1. [Accounting Service](acct-service/README.md)
   1. [Accounting Service Counter](counter/README.md)
2. [Accounting Enricher Job](acct-enricher-job/README.md)
3. [Accounting Enricher Job Http](acct-enricher-job-http/README.md)

###  Tests
You must have `JAVA 8` to run the tests.
```bash
mvn clean test
```
