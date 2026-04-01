# emailEchoKafka
# Email-Echo 

### Getting Started

Email echo application listens to the mailbox, extracts content, and pushes it to Kafka using producers and consumers.

### Mail Configuration

Update below mail properties in the ```application.properties``` file.

```agsl
mail.imaps.username=<mail-address>
mail.imaps.password=<app-password>
```

### Kafka Configuration

  * Version - 3.5
  * Zookeeper Image - wurstmeister/zookeeper
  * Kafka Image - wurstmeister/kafka

#### Create Containers

```docker compose -d```

#### Move into Kafka container

```docker exec -it <kafka_conatiner_id> /bin/sh```

#### Go inside kafka installation folder

```cd /opt/kafka_<version>/bin```

#### Create Kafka topic

```kafka-topics.sh --create --zookeeper zookeeper:2181 --replication-factor 1 --partitions 1 --topic quickstart```

#### Start Producer CLI
