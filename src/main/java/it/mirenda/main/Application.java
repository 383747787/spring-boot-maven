package it.mirenda.main;

import java.util.Arrays;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaReceiverInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import scala.Tuple2;


/**
 * @author <a href="mailto:francesco.mirenda@finconsgroup.com">Francesco L Mirenda</a>
 *
 */
@SpringBootApplication
public class Application {

	private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

	//	public static void main(String[] args) {
	//		ConfigurableApplicationContext context = SpringApplication.run(Application.class, args);
	//		
	//		LOGGER.debug("Spring boot initialized. Inspecting Beans...");
	//		
	//		String[] beanDefinitionNames = context.getBeanDefinitionNames();
	//		
	//		List<String> asList = Arrays.asList(beanDefinitionNames);
	//		Collections.sort(asList);
	//		
	//		asList.stream().forEach( bean -> LOGGER.info("Bean name [{}]", bean ));
	//		
	//		context.close();
	//		
	//	}

	public static void main(String[] args) {
		LOGGER.debug("INIZIO");
		SpringApplication.run(Application.class, args);
		SparkConf sparkConf = new SparkConf().setMaster("local[2]").setAppName("NetworkWordCount");
		JavaStreamingContext streamingContext = new JavaStreamingContext(sparkConf, Durations.seconds(1));

		// Create a DStream that will connect to hostname:port, like localhost:9999
		JavaReceiverInputDStream<String> lines = streamingContext.socketTextStream("localhost", 9999);

		// Split each line into words
		JavaDStream<String> words = lines.
				flatMap( x -> Arrays.asList(x.split(" ")) );

		// Count each word in each batch
		JavaPairDStream<String, Integer> pairs = words.
				mapToPair( s -> new Tuple2<String, Integer>(s, 1) );

		JavaPairDStream<String, Integer> wordCounts = pairs.
				reduceByKey( (i1, i2) -> i1 + i2 );

		// Print the first ten elements of each RDD generated in this DStream to the console
		LOGGER.info("Words count -> [{}]", wordCounts.toString());
		
		wordCounts.print();
		

		streamingContext.start();              // Start the computation
		streamingContext.awaitTermination();   // Wait for the computation to terminate
	}

}
