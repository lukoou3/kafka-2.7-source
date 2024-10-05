package org.apache.kafka.clients.consumer;

import com.sun.tools.javac.util.List;
import org.apache.kafka.common.TopicPartition;

import java.time.Duration;
import java.util.Properties;

public class MyKafkaConsumerTest {

    public static void main(String[] args) throws Exception {
        testAssignConsumer();
    }

    public static void testAssignConsumer() throws Exception {
        Properties props = new Properties();
        props.put("bootstrap.servers", "192.168.216.86:9092");
        props.put("group.id", "test");
        props.put("enable.auto.commit", "true");
        props.put("auto.offset.reset", "latest");
        props.put("kafka.session.timeout.ms", "60000");
        props.put("max.poll.records", "1000");
        props.put("receive.buffer.bytes", "2097152");
        props.put("max.partition.fetch.bytes", "4194304");
        props.put("fetch.min.bytes", "10240");
        props.put("fetch.max.wait.ms", "600");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

        String topic = "logs";
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        List<TopicPartition> partitions = List.of(new TopicPartition(topic, 0));
        consumer.assign(partitions);

        long begin = System.currentTimeMillis();
        long end = begin + 1000 * 60 * 10;
        while (System.currentTimeMillis() >= end){
            ConsumerRecords<String, String> records =  consumer.poll(Duration.ofMillis(500));
            for (ConsumerRecord<String, String> record : records) {
                System.out.println(record);
            }
        }

        consumer.close();
    }

}
