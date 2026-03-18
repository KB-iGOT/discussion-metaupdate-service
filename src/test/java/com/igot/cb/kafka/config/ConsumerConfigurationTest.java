package com.igot.cb.kafka.config;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.poi.ss.formula.functions.T;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;


class ConsumerConfigurationTest {

    private ConsumerConfiguration config;

    private AdminClient mockAdminClient;


    @BeforeEach
    void setUp() {
        mockAdminClient = mock(AdminClient.class);
        config = new ConsumerConfiguration(mockAdminClient);
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



    @Test
    void testIsKafkaHealthy_success() throws Exception {
        // Arrange
        ListTopicsResult mockResult = mock(ListTopicsResult.class);
        KafkaFuture<Set<String>> kafkaFuture = KafkaFuture.completedFuture(Set.of("topic1"));

        when(mockAdminClient.listTopics()).thenReturn(mockResult);
        when(mockResult.names()).thenReturn(kafkaFuture);



        // Act
        boolean healthy = config.isKafkaHealthy();

        // Assert
        assertTrue(healthy);
        verify(mockAdminClient).listTopics();
    }

    @Test
    void testIsKafkaHealthy_failure_alternate() throws Exception {
        // Arrange
        ListTopicsResult mockResult = mock(ListTopicsResult.class);
        KafkaFuture<Set<String>> mockFuture = mock(KafkaFuture.class);

        when(mockAdminClient.listTopics()).thenReturn(mockResult);
        when(mockResult.names()).thenReturn(mockFuture);

        // Here we simulate get() throwing an exception
        when(mockFuture.get(anyLong(), any())).thenThrow(new ExecutionException("Kafka down", new RuntimeException()));

        // Act
        boolean healthy = config.isKafkaHealthy();

        // Assert
        assertFalse(healthy);

        verify(mockAdminClient).listTopics();
        verify(mockResult).names();
        verify(mockFuture).get(anyLong(), any());
    }

}
