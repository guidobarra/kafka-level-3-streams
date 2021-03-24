package kafka.streams;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.*;

import java.util.Arrays;
import java.util.Properties;

import static java.lang.System.*;

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
public class ColoursFavoriteApp {

    private static final String TOPIC_INPUT_FAVORITE_COLOUR = "favorite-colours-input";

    private static final String TOPIC_OUTPUT_FAVORITE_COLOUR = "favorite-colours-output";

    private static final String TOPIC_INTERMEDIATE_COLOUR_FAVORITE_USER = "colours-favorite-user";

    private static final String APPLICATION_ID = "count-colour-favorite-app";

    private static final String BOOTSTRAP_SERVER = "localhost:9092";

    private static final String EARLIEST = "earliest";

    private static final String COUNTS_BY_COLOURS = "CountsByColour";

    private static final String COMA = ",";

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
        do {
            streams.localThreadsMetadata().forEach(out::println);
            try {
                Thread.sleep(5000);
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        } while (true);

    }

    public static Topology createTopology(){
        StreamsBuilder builder = new StreamsBuilder();

        // 1 - stream from Kafka
        KStream<String, String> textLines = builder.stream(TOPIC_INPUT_FAVORITE_COLOUR);

        KStream<String, String> userByColourStream = textLines
                .filter((key, value) -> value.contains(COMA))
                .selectKey((key, value) -> value.split(COMA)[0].toLowerCase())
                .mapValues((key, value) -> value.split(COMA)[1].toLowerCase())
                .filter((key, value) -> Arrays.asList("red", "green", "blue").contains(value));

       userByColourStream.to(TOPIC_INTERMEDIATE_COLOUR_FAVORITE_USER);

       KTable<String, String> userByColourTable = builder.table(TOPIC_INTERMEDIATE_COLOUR_FAVORITE_USER);

        KTable<String, Long> colourFavorite = userByColourTable
               .groupBy((user, colour) -> new KeyValue<>(colour, colour))
               .count(Materialized.as(COUNTS_BY_COLOURS));

        colourFavorite.toStream().to(TOPIC_OUTPUT_FAVORITE_COLOUR, Produced.with(Serdes.String(), Serdes.Long()));
        return builder.build();
    }
}
