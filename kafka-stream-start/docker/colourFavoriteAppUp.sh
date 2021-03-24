#!/bin/bash

#create containers zookeeper and kafka
docker-compose -f kafka.yml up -d

sleep 10

#create topic favorite-colours-input
echo 'creating topic favorite-colours-input'
docker exec -it kafka kafka-topics \
                      --zookeeper zookeeper:2181 \
                      --create --topic favorite-colours-input \
                      --replication-factor 1 \
                      --partitions 3 \
                      --if-not-exists

#create topic favorite-colours-output
echo 'creating topic favorite-colours-output'
docker exec -it kafka kafka-topics \
                      --zookeeper zookeeper:2181 \
                      --create --topic favorite-colours-output \
                      --replication-factor 1 \
                      --partitions 3 \
                      --config cleanup.policy=compact \
                      --if-not-exists

#create topic colours-favorite-user
echo 'creating topic colours-favorite-user'
docker exec -it kafka kafka-topics \
                      --zookeeper zookeeper:2181 \
                      --create --topic colours-favorite-user \
                      --replication-factor 1 \
                      --partitions 3 \
                      --config cleanup.policy=compact \
                      --if-not-exists

#consumer
echo 'show topic word-count-output, consumer'
docker exec -it kafka kafka-console-consumer --bootstrap-server kafka:9092 \
                                             --topic favorite-colours-output \
                                             --from-beginning \
                                             --formatter kafka.tools.DefaultMessageFormatter \
                                             --property print.key=true \
                                             --property print.value=true \
                                             --property key.deserializer=org.apache.kafka.common.serialization.StringDeserializer \
                                             --property value.deserializer=org.apache.kafka.common.serialization.LongDeserializer

#write topic input, name topic favorite-colours-input
#execute other console bash
#docker exec -it kafka kafka-console-producer --broker-list kafka:9092 --topic favorite-colours-input

#EXAMPLE
#guido,blue
#lucia,green
#guido,ReD
#jony,RED