package com.igot.cb.kafka.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;

import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


class ConsumerConfigurationTest {

    private ConsumerConfiguration config;

    @BeforeEach
    void setUp() {
        config = new ConsumerConfiguration();

        // Set @Value fields
        config.getClass().getDeclaredFields();
        setField(config, "kafkabootstrapAddress", "localhost:9092");
        setField(config, "kafkaOffsetResetValue", "earliest");
        setField(config, "kafkaMaxPollInterval", 300000);
        setField(config, "kafkaMaxPollRecords", 500);
        setField(config, "kafkaAutoCommitInterval", 1000);
    }

    @Test
    void testConsumerConfigs() {
        Map<String, Object> props = config.consumerConfigs();
        assertThat(props).isNotNull();
        assertEquals("localhost:9092", props.get(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG));
        assertEquals(true, props.get(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG));
        assertEquals(1000, props.get(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG));
        assertEquals("earliest", props.get(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG));
        assertEquals(300000, props.get(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG));
        assertEquals(500, props.get(ConsumerConfig.MAX_POLL_RECORDS_CONFIG));
        assertEquals(StringDeserializer.class, props.get(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG));
        assertEquals(StringDeserializer.class, props.get(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG));
    }

    @Test
    void testConsumerFactory() {
        ConsumerFactory<String, String> factory = config.consumerFactory();
        assertThat(factory).isNotNull();
    }

    @Test
    void testKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                (ConcurrentKafkaListenerContainerFactory<String, String>) config.kafkaListenerContainerFactory();
        assertThat(factory).isNotNull();
        assertThat(factory.getContainerProperties().getPollTimeout()).isEqualTo(3000);
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
