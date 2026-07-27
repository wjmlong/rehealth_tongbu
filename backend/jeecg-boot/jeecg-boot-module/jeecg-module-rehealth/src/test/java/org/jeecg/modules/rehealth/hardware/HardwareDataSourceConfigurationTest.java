package org.jeecg.modules.rehealth.hardware;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class HardwareDataSourceConfigurationTest {
    @Test
    void keepsTheRoutingDataSourceAsTheOnlyDataSourceBeanWhenHardwarePersistenceIsEnabled() {
        JdbcDataSource hardwareDataSource = new JdbcDataSource();
        hardwareDataSource.setURL("jdbc:h2:mem:hardware-config;MODE=MySQL");
        DynamicRoutingDataSource routingDataSource = new DynamicRoutingDataSource(List.of());
        routingDataSource.addDataSource(HardwareDataSourceConfiguration.HARDWARE_DATA_SOURCE_NAME, hardwareDataSource);

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                    "hardware-test",
                    Map.of("rehealth.hardware-db.enabled", "true")
            ));
            context.registerBean("dataSource", DynamicRoutingDataSource.class, () -> routingDataSource);
            context.register(HardwareDataSourceConfiguration.class);

            assertDoesNotThrow(context::refresh);
            assertEquals(Set.of("dataSource"), Set.of(context.getBeanNamesForType(DataSource.class)));
            JdbcTemplate jdbcTemplate = context.getBean("rehealthHardwareJdbcTemplate", JdbcTemplate.class);
            assertSame(hardwareDataSource, jdbcTemplate.getDataSource());
        }
    }

    @Test
    void providesSeparateSoftwareAndHardwareJdbcTemplates() {
        JdbcDataSource masterDataSource = new JdbcDataSource();
        masterDataSource.setURL("jdbc:h2:mem:software-config;MODE=MySQL");
        JdbcDataSource hardwareDataSource = new JdbcDataSource();
        hardwareDataSource.setURL("jdbc:h2:mem:hardware-config-pair;MODE=MySQL");
        DynamicRoutingDataSource routingDataSource = new DynamicRoutingDataSource(List.of());
        routingDataSource.setPrimary("master");
        routingDataSource.addDataSource("master", masterDataSource);
        routingDataSource.addDataSource(HardwareDataSourceConfiguration.HARDWARE_DATA_SOURCE_NAME, hardwareDataSource);

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                    "persistence-test",
                    Map.of(
                            "rehealth.hardware-db.enabled", "true",
                            "rehealth.software-db.enabled", "true"
                    )
            ));
            context.registerBean("dataSource", DynamicRoutingDataSource.class, () -> routingDataSource);
            context.scan("org.jeecg.modules.rehealth.hardware");

            assertDoesNotThrow(context::refresh);
            JdbcTemplate softwareTemplate = context.getBean("rehealthSoftwareJdbcTemplate", JdbcTemplate.class);
            JdbcTemplate hardwareTemplate = context.getBean("rehealthHardwareJdbcTemplate", JdbcTemplate.class);
            assertSame(routingDataSource, softwareTemplate.getDataSource());
            assertSame(hardwareDataSource, hardwareTemplate.getDataSource());
        }
    }
}
