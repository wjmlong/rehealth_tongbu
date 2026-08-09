package org.jeecg.config.flyway;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import jakarta.annotation.PostConstruct;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

/**
 * Runs the Cloud-only ReHealth migration against the Jeecg dynamic master.
 * Migration failures deliberately propagate and abort application startup.
 */
@Configuration
public class RehealthCloudFlywayConfig {
    static final String MASTER_NAME = "master";
    static final String BASELINE_VERSION = "3.9.2.0";

    private final DataSource routingDataSource;
    private final Environment environment;

    @Value("${spring.flyway.enabled:false}")
    private boolean enabled;

    @Value("${spring.flyway.locations:classpath:flyway/sql/mysql}")
    private String locations;

    @Value("${spring.flyway.baseline-version:3.9.2.0}")
    private String baselineVersion;

    public RehealthCloudFlywayConfig(DataSource routingDataSource, Environment environment) {
        this.routingDataSource = routingDataSource;
        this.environment = environment;
    }

    @PostConstruct
    public void migrate() {
        if (!enabled) {
            return;
        }
        String masterUrl = environment.getProperty(
                "spring.datasource.dynamic.datasource.master.url", "");
        DataSource master = requireMysqlMaster(routingDataSource, masterUrl);
        migrateMaster(master, locations, baselineVersion);
    }

    static DataSource requireMysqlMaster(DataSource routingDataSource, String masterUrl) {
        if (!(routingDataSource instanceof DynamicRoutingDataSource dynamicDataSource)) {
            throw new IllegalStateException("Cloud Flyway requires the Jeecg dynamic routing datasource");
        }
        DataSource master = dynamicDataSource.getDataSources().get(MASTER_NAME);
        if (master == null) {
            throw new IllegalStateException("Cloud Flyway requires named datasource 'master'");
        }
        if (masterUrl == null || !masterUrl.strip().toLowerCase(java.util.Locale.ROOT)
                .startsWith("jdbc:mysql:")) {
            throw new IllegalStateException("Cloud Flyway master datasource must be MySQL");
        }
        return master;
    }

    static int migrateMaster(DataSource master, String locations) {
        return migrateMaster(master, locations, BASELINE_VERSION);
    }

    static int migrateMaster(DataSource master, String locations, String baselineVersion) {
        Flyway flyway = Flyway.configure()
                .dataSource(master)
                .locations(locations)
                .baselineOnMigrate(true)
                .baselineVersion(MigrationVersion.fromVersion(baselineVersion))
                .ignoreMissingMigrations(true)
                .validateOnMigrate(true)
                .cleanDisabled(true)
                .load();
        return flyway.migrate().migrationsExecuted;
    }
}
