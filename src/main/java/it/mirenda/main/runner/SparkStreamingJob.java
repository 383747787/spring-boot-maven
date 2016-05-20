package it.mirenda.main.runner;

import java.util.Arrays;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaReceiverInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.mirenda.main.Application;
import scala.Tuple2;

/**
 * @author <a href="mailto:francesco.mirenda@finconsgroup.com">Francesco L Mirenda</a>
 *
 */
public class SparkStreamingJob {

	private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

	{
		LOGGER.debug("INIZIO");
		SparkConf sparkConf = new SparkConf().setMaster("local[2]").setAppName("NetworkWordCount");
		JavaStreamingContext streamingContext = new JavaStreamingContext(sparkConf, Durations.seconds(1));

		// Create a DStream that will connect to hostname:port, like localhost:9999
		JavaReceiverInputDStream<String> lines = streamingContext.socketTextStream("localhost", 9999);

		// Split each line into words
		JavaDStream<String> words = lines.flatMap(
				new FlatMapFunction<String, String>() {
					@Override public Iterable<String> call(String x) {
						LOGGER.debug("flatMap called -> [{}]", x);
						return Arrays.asList(x.split(" "));
					}
				});

		// Count each word in each batch
		JavaPairDStream<String, Integer> pairs = words.mapToPair(
				new PairFunction<String, String, Integer>() {
					@Override public Tuple2<String, Integer> call(String s) {
						return new Tuple2<String, Integer>(s, 1);
					}
				});
		JavaPairDStream<String, Integer> wordCounts = pairs.reduceByKey(
				new Function2<Integer, Integer, Integer>() {
					@Override public Integer call(Integer i1, Integer i2) {
						return i1 + i2;
					}
				});

		// Print the first ten elements of each RDD generated in this DStream to the console
		wordCounts.print();

		streamingContext.start();              // Start the computation
		streamingContext.awaitTermination();   // Wait for the computation to terminate
	}
}
