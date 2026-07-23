package com.rehealth.device.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Configuration
@EnableConfigurationProperties(KafkaPublisherProperties.class)
public class KafkaPublisherConfiguration {
    @Bean
    OutboxEventSerializer outboxEventSerializer(ObjectMapper objectMapper) {
        return new OutboxEventSerializer(objectMapper);
    }

    @Bean
    @ConditionalOnProperty(name = "rehealth.kafka.publisher.enabled", havingValue = "true")
    BrokerSender brokerSender(
            KafkaTemplate<String, String> kafkaTemplate,
            KafkaPublisherProperties properties
    ) {
        return (topic, key, value) -> {
            try {
                kafkaTemplate.send(topic, key, value)
                        .get(properties.getAckTimeout().toMillis(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new BrokerPublishException("Kafka acknowledgement interrupted", exception);
            } catch (ExecutionException | TimeoutException exception) {
                throw new BrokerPublishException("Kafka did not acknowledge the event", exception);
            }
        };
    }
}
