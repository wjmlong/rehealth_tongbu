package com.rehealth.device.kafka;

@FunctionalInterface
public interface BrokerSender {
    void send(String topic, String key, String value);
}
