package com.rehealth.device.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.KafkaException;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Configuration
@EnableConfigurationProperties({KafkaPublisherProperties.class, KafkaSecretProperties.class})
public class KafkaPublisherConfiguration {
    @Bean
    OutboxEventSerializer outboxEventSerializer(ObjectMapper objectMapper) {
        return new OutboxEventSerializer(objectMapper);
    }

    @Bean
    @ConditionalOnProperty(name = "rehealth.kafka.publisher.enabled", havingValue = "true")
    ProducerFactory<String, String> rehealthKafkaProducerFactory(
            KafkaProperties kafkaProperties,
            KafkaSecretProperties secrets
    ) {
        Map<String, Object> properties = kafkaProperties.buildProducerProperties(null);
        secrets.applyTo(properties);
        return new DefaultKafkaProducerFactory<>(properties);
    }

    @Bean
    @ConditionalOnProperty(name = "rehealth.kafka.publisher.enabled", havingValue = "true")
    KafkaTemplate<String, String> rehealthKafkaTemplate(
            ProducerFactory<String, String> rehealthKafkaProducerFactory
    ) {
        return new KafkaTemplate<>(rehealthKafkaProducerFactory);
    }

    @Bean
    @ConditionalOnProperty(name = "rehealth.kafka.publisher.enabled", havingValue = "true")
    public BrokerSender brokerSender(
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
            } catch (ExecutionException | TimeoutException | KafkaException exception) {
                throw new BrokerPublishException("Kafka did not acknowledge the event", exception);
            }
        };
    }
}
