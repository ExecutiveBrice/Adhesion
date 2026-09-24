package com.wild.corp.adhesion.shop.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.assertj.core.api.Assertions.assertThat;

class ShopFlywayMigrationTest {

    @Test
    void createsShopSchemaFromAnEmptyDatabase() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:shop-migration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .target("3.1")
                .load()
                .migrate();

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        Integer tableCount = jdbcTemplate.queryForObject("""
                select count(*) from information_schema.tables
                where table_name in (
                    'SHOP_PRODUCTS', 'SHOP_PRODUCT_VARIANTS', 'SHOP_ORDERS',
                    'SHOP_ORDER_ITEMS', 'SHOP_PAYMENTS', 'SHOP_PAYMENT_ATTEMPTS'
                )
                """, Integer.class);

        assertThat(tableCount).isEqualTo(6);

        Integer sequenceCount = jdbcTemplate.queryForObject("""
                select count(*) from information_schema.sequences
                where sequence_name = 'SHOP_ORDER_NUMBER_SEQ'
                """, Integer.class);

        assertThat(sequenceCount).isEqualTo(1);
    }
}
