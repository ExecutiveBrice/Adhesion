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
    void addsSupplierDraftsAndPurchaseAmountsWithoutChangingExistingOrders() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:shop-supplier-drafts;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        new ResourceDatabasePopulator(
                new ClassPathResource("db/migration/V10__create_shop_supplier_orders.sql"),
                new ClassPathResource("db/migration/V12__shop_supplier_order_drafts_and_amounts.sql"))
                .execute(dataSource);
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        // H2 ferme l'état interne des CHECK créés par ResourceDatabasePopulator.
        jdbc.execute("ALTER TABLE shop_supplier_orders DROP CONSTRAINT ck_shop_supplier_order_status");
        jdbc.update("""
                INSERT INTO shop_supplier_orders (supplier_name, status, created_at, updated_at)
                VALUES ('Ancien fournisseur', 'ORDERED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        jdbc.update("""
                INSERT INTO shop_supplier_order_lines (order_id, variant_id, product_name, sku, quantity)
                VALUES (1, 20, 'Tee-shirt', 'TS-M', 3)
                """);

        jdbc.update("""
                INSERT INTO shop_supplier_orders (supplier_name, status, created_at, updated_at)
                VALUES ('Nouveau fournisseur', 'DRAFT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        jdbc.update("""
                INSERT INTO shop_supplier_order_lines (order_id, variant_id, product_name, sku, quantity,
                    unit_cost_amount_cents, unit_cost_currency, expected_need, extra_approved)
                VALUES (2, 20, 'Tee-shirt', 'TS-M', 5, 800, 'EUR', 5, false)
                """);

        assertThat(jdbc.queryForList("SELECT status FROM shop_supplier_orders ORDER BY id", String.class))
                .containsExactly("ORDERED", "DRAFT");
        assertThat(jdbc.queryForObject("SELECT unit_cost_amount_cents FROM shop_supplier_order_lines WHERE order_id = 1",
                Long.class)).isNull();
        assertThat(jdbc.queryForObject("SELECT unit_cost_amount_cents FROM shop_supplier_order_lines WHERE order_id = 2",
                Long.class)).isEqualTo(800L);
    }

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
    void createsShopSchemaForDatabaseBaselinedAfterV1() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:shop-baselined;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");

        new ResourceDatabasePopulator(
                new ClassPathResource("db/migration/V8__create_shop_domain_for_baselined_databases.sql"))
                .execute(dataSource);

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        Integer tableCount = jdbcTemplate.queryForObject("""
                select count(*) from information_schema.tables
                where table_name in (
                    'SHOP_PRODUCTS', 'SHOP_PRODUCT_VARIANTS', 'SHOP_ORDERS',
                    'SHOP_ORDER_ITEMS', 'SHOP_PAYMENTS', 'SHOP_PAYMENT_ATTEMPTS'
                )
                """, Integer.class);

        assertThat(tableCount).isEqualTo(6);
    }

    @Test
    void addsRefundRequestFlagAndConversationStorage() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:shop-refund-request;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");

        new ResourceDatabasePopulator(
                new ClassPathResource("db/migration/V8__create_shop_domain_for_baselined_databases.sql"),
                new ClassPathResource("db/migration/V9__add_shop_refund_requests_and_order_messages.sql"))
                .execute(dataSource);

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        Integer messageTableCount = jdbcTemplate.queryForObject("""
                select count(*) from information_schema.tables where table_name = 'SHOP_ORDER_MESSAGES'
                """, Integer.class);
        Integer refundColumnCount = jdbcTemplate.queryForObject("""
                select count(*) from information_schema.columns
                where table_name = 'SHOP_ORDERS' and column_name = 'REFUND_REQUESTED'
                """, Integer.class);

        assertThat(messageTableCount).isEqualTo(1);
        assertThat(refundColumnCount).isEqualTo(1);
    }

    @Test
    void allowsReservationsAndSalesBeyondRecordedStock() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:shop-stock-orders;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        new ResourceDatabasePopulator(
                new ClassPathResource("db/migration/V8__create_shop_domain_for_baselined_databases.sql"))
                .execute(dataSource);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.update("""
                INSERT INTO shop_products (name, slug, created_at, updated_at)
                VALUES ('Tee-shirt', 'tee-shirt', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("""
                INSERT INTO shop_product_variants (product_id, sku, price_amount_cents, price_currency,
                                                   created_at, updated_at)
                VALUES (1, 'TS-M', 1500, 'EUR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);

        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V11__allow_shop_orders_beyond_stock.sql"))
                .execute(dataSource);
        jdbcTemplate.update("UPDATE shop_product_variants SET stock_reserved = 2 WHERE sku = 'TS-M'");
        jdbcTemplate.update("UPDATE shop_product_variants SET stock_on_hand = -2, stock_reserved = 0 WHERE sku = 'TS-M'");

        assertThat(jdbcTemplate.queryForObject(
                "SELECT stock_tracked FROM shop_product_variants WHERE sku = 'TS-M'", Boolean.class)).isTrue();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT stock_on_hand FROM shop_product_variants WHERE sku = 'TS-M'", Long.class)).isEqualTo(-2);
    }

    @Test
    void allowsOrdersWhenLegacyStockConstraintIsAbsent() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:shop-stock-without-check;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        new ResourceDatabasePopulator(
                new ClassPathResource("db/migration/V8__create_shop_domain_for_baselined_databases.sql"))
                .execute(dataSource);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("ALTER TABLE shop_product_variants DROP CONSTRAINT ck_shop_variant_stock");

        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V11__allow_shop_orders_beyond_stock.sql"))
                .execute(dataSource);
        jdbcTemplate.update("""
                INSERT INTO shop_products (name, slug, created_at, updated_at)
                VALUES ('Tee-shirt', 'tee-shirt', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("""
                INSERT INTO shop_product_variants (product_id, sku, price_amount_cents, price_currency,
                                                   stock_tracked, stock_on_hand, stock_reserved,
                                                   created_at, updated_at)
                VALUES (1, 'TS-L', 1500, 'EUR', true, 0, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT stock_reserved FROM shop_product_variants WHERE sku = 'TS-L'", Long.class)).isEqualTo(2);
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
