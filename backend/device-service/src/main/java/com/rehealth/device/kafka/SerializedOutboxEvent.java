package com.rehealth.device.kafka;

public record SerializedOutboxEvent(String deviceKey, String json) {
}
