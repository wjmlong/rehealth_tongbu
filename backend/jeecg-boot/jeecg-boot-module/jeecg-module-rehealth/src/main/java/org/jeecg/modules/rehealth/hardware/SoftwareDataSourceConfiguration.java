package org.jeecg.modules.rehealth.hardware;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(name = "rehealth.software-db.enabled", havingValue = "true")
public class SoftwareDataSourceConfiguration {
    @Bean("rehealthSoftwareJdbcTemplate")
    public JdbcTemplate rehealthSoftwareJdbcTemplate(
            @Qualifier("dataSource") DataSource routingDataSource
    ) {
        return new JdbcTemplate(routingDataSource);
    }
}
