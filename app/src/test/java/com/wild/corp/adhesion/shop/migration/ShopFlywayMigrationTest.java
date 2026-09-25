package com.wild.corp.adhesion.shop.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

class ShopFlywayMigrationTest {

    @Test
    void initializesItemStatusesFromExistingOrders() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:shop-item-status;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").target("3.1").load().migrate();

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        // H2 2.4 ferme l'état interne du CHECK créé par V1 après la migration Flyway.
        jdbcTemplate.execute("ALTER TABLE shop_orders DROP CONSTRAINT ck_shop_order_status");
        String[] orderStatuses = {"COMPLETED", "CANCELLED", "PROCESSING", "PAID"};
        for (int index = 0; index < orderStatuses.length; index++) {
            int id = index + 1;
            jdbcTemplate.update("""
                    INSERT INTO shop_orders (id, order_number, customer_user_id, status, total_amount_cents,
                                             total_currency, version, created_at, updated_at)
                    VALUES (?, ?, 42, ?, 100, 'EUR', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """, id, "CMD-" + id, orderStatuses[index]);
            jdbcTemplate.update("""
                    INSERT INTO shop_order_items (id, order_id, product_id, product_name, unit_price_amount_cents,
                                                  unit_price_currency, quantity, stock_reserved, line_total_amount_cents,
                                                  line_total_currency, created_at, updated_at)
                    VALUES (?, ?, 10, 'Tee-shirt', 100, 'EUR', 1, false, 100, 'EUR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """, id, id);
        }

        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V7__add_shop_order_item_status.sql"))
                .execute(dataSource);
        assertThat(jdbcTemplate.queryForList("SELECT status FROM shop_order_items ORDER BY id", String.class))
                .containsExactly("COMPLETED", "CANCELLED", "PROCESSING", "PENDING");
    }

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
