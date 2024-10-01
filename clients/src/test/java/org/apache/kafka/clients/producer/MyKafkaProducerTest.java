package org.apache.kafka.clients.producer;

import java.util.Properties;

public class MyKafkaProducerTest {

    public static void main(String[] args) throws Exception {
        testSendTread();
    }

    public static void testSendTread() throws Exception{
        Properties props = new Properties();
        props.put("bootstrap.servers", "192.168.216.86:9092");
        props.put("acks", "1");
        props.put("retries", 0);
        props.put("linger.ms", 1000);
        props.put("compression.type", "snappy");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        String topic = "logs";
        KafkaProducer<String, String> producer = new KafkaProducer<>(props);
        for (int i = 0; i < 20; i++) {
            producer.send(new ProducerRecord<>(topic, String.valueOf(i)));
            Thread.sleep(100);
            producer.send(new ProducerRecord<>(topic, String.valueOf(i)));
            System.out.println(i);
            Thread.sleep(10000);
        }

        producer.close();
    }

}
