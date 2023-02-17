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

import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.*;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

import java.time.Duration;

public class DatagenJob {

    public static final Schema STOCK_SCHEMA = Schema.newBuilder()
            .column("ticker_symbol", DataTypes.STRING().notNull())
            .column("price", DataTypes.DOUBLE())
            .column("s_timestamp", DataTypes.TIMESTAMP_LTZ(3))
            .primaryKey("ticker_symbol")
            .build();

    public static final Schema TRANSACTION_SCHEMA = Schema.newBuilder()
            .column("id", DataTypes.STRING().notNull())
            .column("transaction_ticker_symbol", DataTypes.STRING().notNull())
            .column("shares_purchased", DataTypes.DOUBLE().notNull())
            .column("t_timestamp", DataTypes.TIMESTAMP_LTZ(3).notNull())
            .build();


    public static void main(String[] args) throws Exception {

        Configuration conf = new Configuration();
        StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironment();

        TableEnvironment tableEnv = StreamTableEnvironment.create(env);

        // Create a stock source table
        tableEnv.createTemporaryTable("stock", TableDescriptor.forConnector("upsert-kafka")
                .schema(STOCK_SCHEMA)
                .option("topic", "stock-topic")
                .option("properties.bootstrap.servers", "localhost:9092")
                .option("key.format", "raw")
                .option("value.format", "json")
                .build());

        // Create a transactions source table
        tableEnv.createTemporaryTable("transactions", TableDescriptor.forConnector("kafka")
                .schema(TRANSACTION_SCHEMA)
                .option("topic", "transaction-topic")
                .option("properties.bootstrap.servers", "localhost:9092")
                .option("value.format", "json")
                .option("properties.group.id", "transactions-consumer")
                .option("properties.auto.offset.reset", "latest")
                .build());

        // Generate data for the stock table
        tableEnv.createTemporaryTable("stock_datagen", TableDescriptor.forConnector("datagen")
                .schema(STOCK_SCHEMA)
                .option("rows-per-second", "1")
                .option("fields.ticker_symbol.length", "2")
                .option("fields.price.min", "0.00")
                .option("fields.price.max", "10000.00")
//                .option("fields.s_timestamp.max-past", "150000")
                .build());

        // Generate data for the transactions table
        tableEnv.createTemporaryTable("transactions_datagen", TableDescriptor.forConnector("datagen")
                .schema(TRANSACTION_SCHEMA)
                .option("rows-per-second", "1000")
                .option("fields.transaction_ticker_symbol.length", "2")
                .option("fields.shares_purchased.min", "1")
                .option("fields.shares_purchased.max", "100")
//                .option("fields.t_timestamp.max-past", "300000")
                .build());

        Table stockDatagen = tableEnv.from("stock_datagen");
        Table transactionsDatagen = tableEnv.from("transactions_datagen");

        stockDatagen.insertInto("stock").execute();
        transactionsDatagen.insertInto("transactions").execute();
    }
}

