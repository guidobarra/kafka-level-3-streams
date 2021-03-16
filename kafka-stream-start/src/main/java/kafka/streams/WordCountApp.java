package kafka.streams;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.*;

import java.util.Arrays;
import java.util.Properties;
/*
WORD_COUNT_STREAMS_APP_TOPOLOGY
Escribir un topologua usando DSL de alto nivel en nuestra aplicacion
Recordar que la data en Kafka Streams es <key, value>
	1) Stream from kafka 						<null, "Kafka Kafka Streams">
	2) MapValues lowercase						<null, "kafka kafka streams">
	3) FlatMapValues split by space           	<null, "kafka">, <null, "kafka">, <null, "streams">
	4) SelectKey to apply a key					<"kafka", "kafka">, <"kafka", "kafka">, <"streams", "streams">
	5) GroupByKey before aggregation			(<"kafka", "kafka">, <"kafka", "kafka">), (<"streams", "streams">)
	6) Count occurrences in each group			<"kafka", 2>, <"streams", 1>
	7) To in order to write the results back to kafka  		data point is written to kafka topic
*/
public class WordCountApp {

    private static final String TOPIC_INPUT = "word-count-input";

    private static final String TOPIC_OUTPUT = "word-count-output";

    private static final String APPLICATION_ID = "word-count-application";

    private static final String BOOTSTRAP_SERVER = "localhost:9092";

    private static final String EARLIEST = "earliest";

    private static final String COUNTS = "Counts";

    private static final String REGEX_SPLIT = "\\W+";

    public static void main(String[] args) {
        Properties config = new Properties();
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, APPLICATION_ID);
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVER);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, EARLIEST);
        config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        KafkaStreams streams = new KafkaStreams(createTopology(), config);
        streams.start();

        // shutdown hook to correctly close the streams application
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));

        // Update:
        // print the topology every 10 seconds for learning purposes
        while(true){
            streams.localThreadsMetadata().forEach(System.out::println);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                break;
            }
        }

    }

    public static Topology createTopology(){
        StreamsBuilder builder = new StreamsBuilder();

        // 1 - stream from Kafka
        KStream<String, String> textLines = builder.stream(TOPIC_INPUT);

        KTable<String, Long> wordCounts = textLines
                .mapValues((ValueMapper<String, String>) String::toLowerCase)//2) MapValues lowercase
                .flatMapValues(lowerCaseTextLine -> Arrays.asList(lowerCaseTextLine.split(REGEX_SPLIT)))//3) FlatMapValues split by space
                .selectKey((key, value) -> value)//4) SelectKey to apply a key
                .groupByKey()//5) GroupByKey before aggregation
                .count(Materialized.as(COUNTS));//6) Count occurrences in each group

        // 7 - to in order to write the results back to kafka
        wordCounts.toStream().to(TOPIC_OUTPUT, Produced.with(Serdes.String(), Serdes.Long()));

        return builder.build();
    }
}
