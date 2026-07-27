package org.jeecg.modules.rehealth.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@ConditionalOnProperty(name = "rehealth.kafka.consumer.enabled", havingValue = "true")
@EnableConfigurationProperties(ReHealthKafkaSecretProperties.class)
public class ReHealthKafkaConfiguration {
    @Bean("rehealthKafkaConsumerFactory")
    ConsumerFactory<String, String> rehealthKafkaConsumerFactory(
            KafkaProperties kafkaProperties,
            ReHealthKafkaSecretProperties secrets
    ) {
        var properties = kafkaProperties.buildConsumerProperties(null);
        secrets.applyTo(properties);
        return new DefaultKafkaConsumerFactory<>(properties);
    }

    @Bean("rehealthKafkaProducerFactory")
    ProducerFactory<String, String> rehealthKafkaProducerFactory(
            KafkaProperties kafkaProperties,
            ReHealthKafkaSecretProperties secrets
    ) {
        var properties = kafkaProperties.buildProducerProperties(null);
        secrets.applyTo(properties);
        return new DefaultKafkaProducerFactory<>(properties);
    }

    @Bean("rehealthKafkaTemplate")
    KafkaTemplate<String, String> rehealthKafkaTemplate(
            @Qualifier("rehealthKafkaProducerFactory")
            ProducerFactory<String, String> producerFactory
    ) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    TelemetryEventParser telemetryEventParser(ObjectMapper objectMapper) {
        return new TelemetryEventParser(objectMapper);
    }

    @Bean
    DlqEnvelopeFactory dlqEnvelopeFactory(ObjectMapper objectMapper) {
        return new DlqEnvelopeFactory(objectMapper);
    }

    @Bean("rehealthKafkaListenerContainerFactory")
    ConcurrentKafkaListenerContainerFactory<String, String> rehealthKafkaListenerContainerFactory(
            @Qualifier("rehealthKafkaConsumerFactory")
            ConsumerFactory<String, String> consumerFactory,
            @Qualifier("rehealthKafkaTemplate")
            KafkaTemplate<String, String> kafkaTemplate,
            DlqEnvelopeFactory dlqEnvelopeFactory
    ) {
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                (ConsumerRecord<?, ?> record, Exception exception) -> kafkaTemplate.send(
                        "rehealth.telemetry.dlq.v1",
                        String.valueOf(record.key()),
                        dlqEnvelopeFactory.redacted(String.valueOf(record.value()))),
                new FixedBackOff(500L, 2L));
        errorHandler.setCommitRecovered(true);
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}
