from kafka import KafkaProducer
import json
import random
import datetime
import time
from threading import Thread
import uuid


BROKERS = "localhost:9092"
producer = KafkaProducer(
    bootstrap_servers=BROKERS,
    value_serializer=lambda v: json.dumps(v).encode('utf-8'),
    key_serializer=str.encode,
    retry_backoff_ms=500,
    request_timeout_ms=20000,
    security_protocol='PLAINTEXT')


def generate_ticker_data():
    data = {}
    now = datetime.datetime.now()
    str_now = now.strftime("%Y-%m-%d %H:%M:%S ")
    data['s_timestamp'] = str_now
    data['ticker_symbol'] = random.choice(['AAPL', 'AMZN', 'MSFT', 'INTC', 'TBV'])
    price = random.random() * 100
    data['price'] = round(price, 2)
    return data

def send_ticker_data_in_thread():
    while True:
        ticker_data = generate_ticker_data()
        try:
            future = producer.send("stock-topic", value=ticker_data,key=ticker_data['ticker_symbol'])
            producer.flush()
            record_metadata = future.get(timeout=10)
            print("sent event to Kafka! topic {} partition {} offset {}".format(record_metadata.topic, record_metadata.partition, record_metadata.offset))
            #time.sleep(5)
        except Exception as e:
            print(e.with_traceback())

def generate_transaction_data():
    data = {}
    now = datetime.datetime.now() - datetime.timedelta(seconds=10)
    str_now = now.strftime("%Y-%m-%d %H:%M:%S")
    data['t_timestamp'] = str_now
    data['id'] = str(uuid.uuid4())
    data['transaction_ticker_symbol'] = random.choice(['AAPL', 'AMZN', 'MSFT', 'INTC', 'TBV'])
    shares_purchased = random.random() * 100
    data['shares_purchased'] = round(shares_purchased, 2)
    return data


def send_transaction_data_in_thread():
    while True:
        transaction_data = generate_transaction_data()
        try:
            future = producer.send("transaction-topic", value=transaction_data,key=transaction_data['id'])
            producer.flush()
            record_metadata = future.get(timeout=10)
            print("sent event to Kafka! topic {} partition {} offset {}".format(record_metadata.topic, record_metadata.partition, record_metadata.offset))
            #time.sleep(1)
        except Exception as e:
            print(e.with_traceback())

Thread(target = send_transaction_data_in_thread).start()
Thread(target = send_ticker_data_in_thread).start()
