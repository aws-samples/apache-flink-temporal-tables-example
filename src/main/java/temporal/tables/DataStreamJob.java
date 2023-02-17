/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: MIT-0
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package temporal.tables;

import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.*;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.table.functions.TemporalTableFunction;
import java.util.Properties;
import com.amazonaws.services.kinesisanalytics.runtime.KinesisAnalyticsRuntime;


import java.util.Map;

import static org.apache.flink.table.api.Expressions.$;
import static org.apache.flink.table.api.Expressions.call;

public class DataStreamJob {

	public static void main(String[] args) throws Exception {


		// pass in properties for kafka bootstrap strings, topic names
		Map<String, Properties> applicationProperties = KinesisAnalyticsRuntime.getApplicationProperties();
		Properties kafkaProperties = applicationProperties.getOrDefault("kafkaProperties", new Properties());
		String bootstrapString = (String) kafkaProperties.getOrDefault("bootstrapString", "localhost:9092");
		String stockTopic = (String) kafkaProperties.getOrDefault("stockTopic", "stock-topic");
		String transactionsTopic = (String) kafkaProperties.getOrDefault("transactionTopic", "transaction-topic");
		String outputTopic = (String) kafkaProperties.getOrDefault("outputTopic", "output");



		StreamExecutionEnvironment env;
        TableEnvironment tableEnv;
		Configuration configuration = new Configuration();

		//in case of idle sources
		configuration.setString("table.exec.source.idle-timeout", "500 ms");

        // local only
		if(kafkaProperties.isEmpty()) {


            env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(configuration);
			env.enableCheckpointing(60000, CheckpointingMode.EXACTLY_ONCE);
			tableEnv = StreamTableEnvironment.create(env);
		}
		else
		{


            EnvironmentSettings settings = EnvironmentSettings
                    .newInstance()
                    .inStreamingMode()
                    .withConfiguration(configuration)
                    .build();

            tableEnv = TableEnvironment.create(settings);

		}


		// Create a source table
		tableEnv.createTemporaryTable("stock", TableDescriptor.forConnector("upsert-kafka")
				.schema(Schema.newBuilder()
						.column("ticker_symbol", DataTypes.STRING().notNull())
						.column("price", DataTypes.DOUBLE())
						.column("s_timestamp", DataTypes.TIMESTAMP_LTZ(3))
						.watermark("s_timestamp", "s_timestamp - INTERVAL '1' MINUTE")
						.primaryKey("ticker_symbol")
						.build())
				.option("topic", stockTopic)
				.option("properties.bootstrap.servers", bootstrapString)
				.option("key.format", "raw")
				.option("value.format", "json")
				.build());



		// Create a transactions source table
		tableEnv.createTemporaryTable("transactions", TableDescriptor.forConnector("kafka")
				.schema(Schema.newBuilder()
						.column("id", DataTypes.STRING().notNull())
						.column("transaction_ticker_symbol", DataTypes.STRING())
						.column("shares_purchased", DataTypes.DOUBLE())
						.column("t_timestamp", DataTypes.TIMESTAMP_LTZ(3))
						.columnByExpression("time_now", "PROCTIME()")
						.watermark("t_timestamp", "t_timestamp - INTERVAL '1' MINUTE")
						.build())
				.option("topic", transactionsTopic)
				.option("properties.bootstrap.servers", bootstrapString)
				.option("properties.group.id", "transactions-consumer")
				.option("value.format", "json")
				.option("properties.auto.offset.reset", "latest")
				.build());


		Table stock = tableEnv.from("stock");

		// define temporal table function
		TemporalTableFunction historicalStockPricesLookupFunction = stock.createTemporalTableFunction(
				$("s_timestamp"),
				$("ticker_symbol"));

		tableEnv.createTemporarySystemFunction("historicalStockPricesLookupFunction", historicalStockPricesLookupFunction);

		// join with transactions based on the time attribute and key
		Table transactions = tableEnv.from("transactions");
		Table result = transactions
				.joinLateral(call("historicalStockPricesLookupFunction",
						$("t_timestamp")),
						$("transaction_ticker_symbol").isEqual($("ticker_symbol")))
				.select($("ticker_symbol"),
						$("time_now"),
						$("t_timestamp"),
						$("s_timestamp"),
						$("shares_purchased").times($("price")).as("total_amount"));

		tableEnv.executeSql("CREATE TABLE kafka_output " +
				"(`ticker_symbol` STRING,\n" +
				"`time_now` TIMESTAMP_LTZ(3),\n" +
				"`t_timestamp` TIMESTAMP_LTZ(3),\n" +
				"`s_timestamp` TIMESTAMP_LTZ(3),\n" +
				"`total_amount` DOUBLE)\n"+
				"WITH ('connector' = 'kafka'," +
				"'topic' = '" + outputTopic + "'," +
				"'value.format' = 'json'," +
				"'properties.bootstrap.servers' = '" + bootstrapString + "' );");

		result.insertInto("kafka_output").execute();
	}


}