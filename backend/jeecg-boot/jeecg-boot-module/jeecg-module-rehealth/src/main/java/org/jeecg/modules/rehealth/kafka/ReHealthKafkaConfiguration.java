package org.jeecg.modules.rehealth.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@ConditionalOnProperty(name = "rehealth.kafka.consumer.enabled", havingValue = "true")
public class ReHealthKafkaConfiguration {
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
            ConsumerFactory<String, String> consumerFactory,
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
