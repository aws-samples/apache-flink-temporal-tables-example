# Apache Flink Temporal Tables on Kinesis Data Analytics for Apache Flink

Temporal Tables allow you to match real-time events with historical events to find the right value at the right time.

For example, let's say we have Stream A which contains Stock Market data such as...

```
{
'ticker_symbol': 'ABCD',
'price': 10.0,
's_timestamp': '2023-01-18 17:46:09.191Z'
}

{
'ticker_symbol': 'EFGH',
'price': 9.0,
's_timestamp': '2023-01-18 17:46:10.191Z'
}
...
```

and stock transaction data which looks like:
```
{
  'id': "1",
  "transaction_ticker_symbol": "ABCD",
  "shares_purchased": 2,
  "t_timestamp":"2023-01-18 22:19:11.784Z"
}

{
  'id': "2",
  "transaction_ticker_symbol": "EFGH",
  "shares_purchased": 4,
  "t_timestamp":"2023-01-18 22:19:11.784Z"
}
```

We might want to find out what user `1` paid for stock `ABCD` at the time of transaction. A simple join could find all the matches, but a [temporal table join](https://flink.apache.org/2019/05/14/temporal-tables.html) will show us the point in time transaction price as of the purchase time with respect to the stock info.

Let's try it out. 

## Getting Started (Flink SQL)
You can run the end-to-end Flink SQL Example by standing up an [MSK Cluster](https://aws.amazon.com/msk) as described in [this video](https://www.youtube.com/watch?v=2Qhc6ePu-0M) or [this article](https://docs.aws.amazon.com/kinesisanalytics/latest/java/gs-table-create.html), and then launching a Kinesis Data Analytics Studio application within the same VPC.

Import the zeppelin notebook located [here](src/main/sql/) to run the end to end examples. Make sure you replace the Bootstrap Server String with your kafka connection string.

## Getting started (Table API)
To start using this example, you will need to install some prerequisites locally:

### Prerequisites

- [Java](https://www.java.com/en/download/help/download_options.html)
- [Maven](https://maven.apache.org/install.html)
- [Git](https://github.com/git-guides/install-git)
- [Docker Desktop](https://www.docker.com/products/docker-desktop/)
- [Apache Kafka](https://kafka.apache.org/downloads)
- [Apache Flink](https://nightlies.apache.org/flink/flink-docs-master/docs/try-flink/local_installation/)

## How do I use this repo?

- To get started with this repo, first ensure you have the [necessary prerequisites](#prerequisites) installed.

### Terminal Commands

- Clone this git repository to your local machine.

```bash
git clone <<git-repo>>
```
- Once in the project root, open a terminal session and navigate to the src/main/supplemental-resources page.

NOTE: For Apache Kafka commands you can modify your PATH variable to include kafka shell commands so that you don't need to reference the full Kafka directory for this sample:

```bash
PATH="$PATH:~/kafka_2.13-2.8.1/bin"
```

- From here you can paste the following bash commands

```bash
Note: all commands meant to be run from src/main/supplemental-resources directory

# start up kafka
cd kafka-docker
docker-compose up -d

#create kafka topics
kafka-topics.sh --create --topic stock-topic --bootstrap-server localhost:9092 --partitions 3 --replication-factor 2 --config "cleanup.policy=compact"  --config "delete.retention.ms=100"  --config "segment.ms=100" --config "min.cleanable.dirty.ratio=0.01"
kafka-topics.sh --create --topic transaction-topic --bootstrap-server localhost:9092 --partitions 3 --replication-factor 2
kafka-topics.sh --create --topic output --bootstrap-server localhost:9092 --partitions 3 --replication-factor 2
```

We've just started up a docker container running Kafka and created 3 topics--:
- stock-topic
- transaction-topic
- output

Each with 3 partitions and a replication factor of 3

- Next, run the DatagenJob.java class found in `src/main/java/temporal.tables/` to begin generating data into your `stock-topic` and `transaction-topic` topics. 

Note: Running from IntelliJ is simple to setup and has many plugins for running Apache Flink

- Finally, run the DataStreamJob.java class found in `src/main/java/temporal.tables` to run  your temporal table join of the two topics and write them out to the `output` topic. 

When run locally, you can view this running job at https://localhost:8081.


- To see output, run the following command from the terminal:



```bash
# view output
kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic output
```

<!-- blank line -->
<figure class="video_container">
  <video controls="true" allowfullscreen="true" poster="path/to/poster_image.png">
    <source src="/src/main/supplemental-resources/temporal-tables.mp4" type="video/mp4">
  </video>
</figure>



### Deploying and Running on KDA

In order to run this example on KDA, you will first need to [create an MSK Cluster], and then create the KDA application within the same VPC. Ensure that you have the right networking setup so that KDA can connect to MSK.
=======
  <iframe src="https://gitlab.aws.dev/jdber/temporal-table-function-kda/-/raw/main/src/main/supplemental-resources/temporal-tables.mp4" frameborder="0" allowfullscreen="true"> </iframe>
</figure>
<!-- blank line -->


### To deploy onto Kinesis Data Analytics for Apache Flink
In order to deploy your application, you can follow [these steps](https://catalog.us-east-1.prod.workshops.aws/workshops/429cec9e-3222-4943-82f7-1f45c45ed99a/en-US/4-packagingforkda) for best practices and how to package / deploy. Ensure you set the appropriate values for the Kafka Brokers, topics, etc rather than using `localhost:9092`. 

Included in the project is also a [sample KDA Studio notebook](src/main/supplemental-resources/kafka-admin.zpln) for producing / consuming data into your MSK Cluster once deployed onto your AWS Infrastructure.
