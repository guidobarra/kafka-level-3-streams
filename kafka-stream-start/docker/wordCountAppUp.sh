#!/bin/bash
#create containers zookeeper and kafka
docker-compose -f kafka.yml up -d

sleep 10

#create topic word-count-input
echo 'creating topic word-count-input'
docker exec -it kafka kafka-topics --zookeeper zookeeper:2181 --create --topic word-count-input --replication-factor 1 --partitions 3 --if-not-exists

#create topic word-count-input
echo 'creating topic word-count-output'
docker exec -it kafka kafka-topics --zookeeper zookeeper:2181 --create --topic word-count-output --replication-factor 1 --partitions 3 --if-not-exists

#consumer
echo 'show topic word-count-output, consumer'
docker exec -it kafka kafka-console-consumer --bootstrap-server kafka:9092 \
                                             --topic word-count-output \
                                             --from-beginning \
                                             --formatter kafka.tools.DefaultMessageFormatter \
                                             --property print.key=true \
                                             --property print.value=true \
                                             --property key.deserializer=org.apache.kafka.common.serialization.StringDeserializer \
                                             --property value.deserializer=org.apache.kafka.common.serialization.LongDeserializer

#write topic input, name topic word-count-input
#execute other console bash
#docker exec -it kafka kafka-console-producer --broker-list kafka:9092 --topic word-count-input

