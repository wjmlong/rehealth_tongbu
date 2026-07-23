package com.rehealth.device.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "rehealth.kafka.publisher")
public class KafkaPublisherProperties {
    private boolean enabled;
    private int batchSize = 100;
    private Duration ackTimeout = Duration.ofSeconds(10);
    private Duration initialBackoff = Duration.ofSeconds(1);
    private Duration maximumBackoff = Duration.ofMinutes(5);
    private String persistedTopic = "rehealth.telemetry.persisted.v1";
    private String qualityTopic = "rehealth.telemetry.quality.v1";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    public Duration getAckTimeout() { return ackTimeout; }
    public void setAckTimeout(Duration ackTimeout) { this.ackTimeout = ackTimeout; }
    public Duration getInitialBackoff() { return initialBackoff; }
    public void setInitialBackoff(Duration initialBackoff) { this.initialBackoff = initialBackoff; }
    public Duration getMaximumBackoff() { return maximumBackoff; }
    public void setMaximumBackoff(Duration maximumBackoff) { this.maximumBackoff = maximumBackoff; }
    public String getPersistedTopic() { return persistedTopic; }
    public void setPersistedTopic(String persistedTopic) { this.persistedTopic = persistedTopic; }
    public String getQualityTopic() { return qualityTopic; }
    public void setQualityTopic(String qualityTopic) { this.qualityTopic = qualityTopic; }
}
