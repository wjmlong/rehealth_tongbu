package com.rehealth.device.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("rehealth.hardware-db")
public class TimescaleDatabaseProperties {
    private String url = "";
    private String username = "";
    private String password = "";
    private String passwordFile = "";
    private final Retention retention = new Retention();

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPasswordFile() {
        return passwordFile;
    }

    public void setPasswordFile(String passwordFile) {
        this.passwordFile = passwordFile;
    }

    public Retention getRetention() {
        return retention;
    }

    public static final class Retention {
        private int measurementDays = 730;
        private int signalMetadataDays = 90;
        private int operationalDays = 1095;
        private int publishedOutboxDays = 30;

        public int getMeasurementDays() {
            return measurementDays;
        }

        public void setMeasurementDays(int measurementDays) {
            this.measurementDays = measurementDays;
        }

        public int getSignalMetadataDays() {
            return signalMetadataDays;
        }

        public void setSignalMetadataDays(int signalMetadataDays) {
            this.signalMetadataDays = signalMetadataDays;
        }

        public int getOperationalDays() {
            return operationalDays;
        }

        public void setOperationalDays(int operationalDays) {
            this.operationalDays = operationalDays;
        }

        public int getPublishedOutboxDays() {
            return publishedOutboxDays;
        }

        public void setPublishedOutboxDays(int publishedOutboxDays) {
            this.publishedOutboxDays = publishedOutboxDays;
        }
    }
}
