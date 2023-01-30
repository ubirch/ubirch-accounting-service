# Local Test with acct-service

1. run docker images

```
cd acct-service
docker-compose up
```

2. migrate cassandra

```
cd acct-service/src/main/resources
cassandra-migrate -H 127.0.0.1 -p 9042 migrate
```

3. create a kafka topic (optional)

The acct-service consumes the `ubirch-acct-evt-json` topic. If you want to define this topic with some partitions, you need to create a topic manually.
Go to [Kafkadrop console](http://localhost:9000) and create the `ubirch-acct-evt-json` topic with number of partitions that you want to use (normally 6 partitions).

4. push messages to Kafka

Run [ServiceTest.scala](../acct-service/src/test/scala/com/ubirch/testers/ServiceTest.scala). You can change the number of message by changing the `batch` variable.
The default is 1M.
```scala
val batch = 1000000
```

5. consume messages from Kafka

When you run the `acct-service`, it starts subscribing messages from the `ubirch-acct-evt-json` topic.
```
cd acct-service
mvn exec:java -Dexec.mainClass=com.ubirch.Service
```
