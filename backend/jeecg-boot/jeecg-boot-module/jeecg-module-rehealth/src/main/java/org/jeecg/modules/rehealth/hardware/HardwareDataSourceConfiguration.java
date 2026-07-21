package org.jeecg.modules.rehealth.hardware;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(name = "rehealth.hardware-db.enabled", havingValue = "true")
public class HardwareDataSourceConfiguration {
    public static final String HARDWARE_DATA_SOURCE_NAME = "hardware";

    @Bean("rehealthHardwareDataSource")
    public DataSource rehealthHardwareDataSource(DataSource routingDataSource) {
        if (!(routingDataSource instanceof DynamicRoutingDataSource dynamicDataSource)) {
            throw new IllegalStateException("ReHealth hardware_db requires the Jeecg dynamic routing datasource");
        }
        if (!dynamicDataSource.getDataSources().containsKey(HARDWARE_DATA_SOURCE_NAME)) {
            throw new IllegalStateException("Named datasource 'hardware' is required when rehealth.hardware-db.enabled=true");
        }
        return dynamicDataSource.getDataSources().get(HARDWARE_DATA_SOURCE_NAME);
    }

    @Bean("rehealthHardwareJdbcTemplate")
    public JdbcTemplate rehealthHardwareJdbcTemplate(
            @Qualifier("rehealthHardwareDataSource") DataSource hardwareDataSource
    ) {
        return new JdbcTemplate(hardwareDataSource);
    }

    @Bean("rehealthHardwareTransactionTemplate")
    public TransactionTemplate rehealthHardwareTransactionTemplate(
            @Qualifier("rehealthHardwareDataSource") DataSource hardwareDataSource
    ) {
        return new TransactionTemplate(new DataSourceTransactionManager(hardwareDataSource));
    }
}
