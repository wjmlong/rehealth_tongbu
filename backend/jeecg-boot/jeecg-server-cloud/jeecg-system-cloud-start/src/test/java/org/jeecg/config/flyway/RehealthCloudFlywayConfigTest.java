package org.jeecg.config.flyway;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RehealthCloudFlywayConfigTest {
    @Test
    void selectsOnlyNamedMysqlMasterAndFailsClosedOtherwise() {
        JdbcDataSource master = dataSource("master-selection");
        DynamicRoutingDataSource routing = new DynamicRoutingDataSource(List.of());
        routing.addDataSource("master", master);
        routing.addDataSource("hardware", dataSource("hardware-selection"));

        assertSame(master, RehealthCloudFlywayConfig.requireMysqlMaster(
                routing, "jdbc:mysql://software-db:3306/rehealth_software"));
        assertThrows(IllegalStateException.class, () ->
                RehealthCloudFlywayConfig.requireMysqlMaster(routing, "jdbc:postgresql://hardware"));
        assertThrows(IllegalStateException.class, () ->
                RehealthCloudFlywayConfig.requireMysqlMaster(master, "jdbc:mysql://software-db/db"));
    }

    @Test
    void baselinesExistingSchemaThenExecutesOnlyCurrentPermissionMigration() {
        JdbcDataSource master = dataSource("migration");
        JdbcTemplate jdbc = new JdbcTemplate(master);
        jdbc.execute("""
                CREATE TABLE sys_permission (
                    id VARCHAR(64) PRIMARY KEY, parent_id VARCHAR(64), name VARCHAR(255),
                    url VARCHAR(255), component VARCHAR(255), is_route INT,
                    component_name VARCHAR(255), redirect VARCHAR(255), menu_type INT,
                    perms VARCHAR(255), perms_type VARCHAR(8), sort_no DECIMAL(10,2),
                    always_show INT, icon VARCHAR(255), is_leaf INT, keep_alive INT,
                    hidden INT, hide_tab INT, description VARCHAR(500), create_by VARCHAR(64),
                    create_time TIMESTAMP, update_by VARCHAR(64), update_time TIMESTAMP,
                    del_flag INT, rule_flag INT, status VARCHAR(8), internal_or_external INT
                )
                """);

        assertEquals(1, RehealthCloudFlywayConfig.migrateMaster(
                master, "classpath:flyway/sql/mysql"));
        assertEquals(1, jdbc.queryForObject(
                "SELECT count(*) FROM sys_permission WHERE perms='rehealth:admin:patient:view'",
                Integer.class));
        assertEquals("3.9.2.0", jdbc.queryForObject(
                "SELECT \"version\" FROM \"flyway_schema_history\" WHERE \"type\"='BASELINE'",
                String.class));
        assertEquals(1, jdbc.queryForObject(
                "SELECT count(*) FROM \"flyway_schema_history\" WHERE \"version\"='3.9.2.1' AND \"success\"=true",
                Integer.class));
    }

    private static JdbcDataSource dataSource(String name) {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + name + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        return dataSource;
    }
}
