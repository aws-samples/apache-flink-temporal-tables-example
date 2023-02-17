#/*
# * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# * SPDX-License-Identifier: MIT-0
# *
# * Permission is hereby granted, free of charge, to any person obtaining a copy of this
# * software and associated documentation files (the "Software"), to deal in the Software
# * without restriction, including without limitation the rights to use, copy, modify,
# * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
# * permit persons to whom the Software is furnished to do so.
# *
# * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
# * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
# * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
# * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
# * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
# * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
# */

# all commands meant to be run from src/main/supplemental-resources directory

# start up kafka
cd kafka-docker
docker-compose up -d
cd ..

#create kafka topics
kafka-topics.sh --create --topic stock-topic --bootstrap-server localhost:9092 --partitions 3 --replication-factor 2 --config "cleanup.policy=compact"  --config "delete.retention.ms=100"  --config "segment.ms=100" --config "min.cleanable.dirty.ratio=0.01"
kafka-topics.sh --create --topic transaction-topic --bootstrap-server localhost:9092 --partitions 3 --replication-factor 2
kafka-topics.sh --create --topic output --bootstrap-server localhost:9092 --partitions 3 --replication-factor 2

# start loading ticker information
# run datagen
# run data stream from IntelliJ

# view output
kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic output
