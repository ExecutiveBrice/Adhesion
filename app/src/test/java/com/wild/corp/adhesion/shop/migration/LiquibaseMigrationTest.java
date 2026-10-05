package com.wild.corp.adhesion.shop.migration;

import jakarta.persistence.Entity;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.util.HashMap;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LiquibaseMigrationTest {
    private static final String MASTER = "db/changelog/db.changelog-master.xml";
    private static final String V1 = "db/changelog/changeset-v1.xml";
    private static final String V2 = "db/changelog/changeset-v2.xml";
    private final List<SingleConnectionDataSource> embeddedDatabases = new ArrayList<>();

    @AfterEach
    void closeEmbeddedDatabases() {
        embeddedDatabases.forEach(SingleConnectionDataSource::destroy);
    }

    private DriverManagerDataSource database() {
        String url = System.getenv("MIGRATION_TEST_URL");
        if (url != null) {
            return new DriverManagerDataSource(url, "migration", "migration");
        }
        // H2 2.4 CHECK expressions retain the creating session. Keep that
        // connection alive for the scenario so constraints remain usable.
        var source = new SingleConnectionDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL", "sa", "", true);
        embeddedDatabases.add(source);
        return source;
    }

    private void migrate(DriverManagerDataSource source, String changelog) throws Exception {
        try (var connection = source.getConnection()) {
            var database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            try (var liquibase = new Liquibase(changelog, new ClassLoaderResourceAccessor(), database)) {
                liquibase.update(new Contexts(), new LabelExpression());
            }
        }
    }

    private void validateMappings(DriverManagerDataSource source) throws Exception {
        var settings = new HashMap<String, Object>();
        settings.put("hibernate.connection.datasource", source);
        settings.put("hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl");
        settings.put("hibernate.implicit_naming_strategy", "org.springframework.boot.hibernate.SpringImplicitNamingStrategy");
        settings.put("hibernate.hbm2ddl.auto", "validate");
        var registry = new StandardServiceRegistryBuilder().applySettings(settings).build();
        try {
            var metadata = new MetadataSources(registry);
            var scanner = new ClassPathScanningCandidateComponentProvider(false);
            scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
            for (var bean : scanner.findCandidateComponents("com.wild.corp.adhesion")) {
                metadata.addAnnotatedClass(Class.forName(bean.getBeanClassName()));
            }
            SchemaManagementToolCoordinator.process(metadata.buildMetadata(), registry, settings, action -> { });
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }

    @Test
    void createsCompleteSchemaAndCanRestartWithoutReapplyingChanges() throws Exception {
        var source = database();
        // PostgreSQL test runs use a disposable database, with a separate schema per scenario.
        isolatePostgres(source);
        migrate(source, MASTER);
        validateMappings(source);
        var jdbc = new JdbcTemplate(source);
        int count = jdbc.queryForObject("SELECT count(*) FROM databasechangelog", Integer.class);
        migrate(source, MASTER);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM databasechangelog", Integer.class)).isEqualTo(count);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM information_schema.tables WHERE table_schema = current_schema() AND lower(table_name) = 'shop_order_messages'", Integer.class)).isEqualTo(1);
    }

    @Test
    void linksActivitiesToExistingSectionsAndCreatesMissingSections() throws Exception {
        var source = database();
        isolatePostgres(source);
        migrate(source, V1);
        var jdbc = new JdbcTemplate(source);
        jdbc.update("""
                INSERT INTO activites (id, groupe_filtre, groupe, prise_en_charge, autorisation_parentale,
                    certificat_medical, charte_amicale, complete, vie_club)
                VALUES (1, 'Yoga', 'NON_COMPETITIVE', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),
                       (2, 'Yoga', 'NON_COMPETITIVE', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),
                       (3, ' Basket ', 'COMPETITION', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),
                       (4, NULL, NULL, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE)
                """);

        migrate(source, V2);

        assertThat(jdbc.queryForList("SELECT s.nom FROM activites a LEFT JOIN sections s ON s.id = a.section_id ORDER BY a.id", String.class))
                .containsExactly("Yoga", "Yoga", "Basket", null);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sections", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT type FROM sections WHERE nom = 'Basket'", String.class)).isEqualTo("COMPETITION");
        assertThatThrownBy(() -> jdbc.update("UPDATE activites SET section_id = 999 WHERE id = 1"))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
    }

    @Test
    void v2ContainsOneXmlOnlyChangesetPerModule() throws Exception {
        String[] modules = {"activity", "shop", "chat", "communication"};
        for (String module : modules) {
            try (var resource = getClass().getClassLoader()
                    .getResourceAsStream("db/changelog/v2/" + module + ".xml")) {
                assertThat(resource).isNotNull();
                String changelog = new String(resource.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                assertThat(changelog).doesNotContain("<sql", "<sqlFile");
                assertThat(changelog.split("<changeSet ", -1)).hasSize(2);
            }
        }

        var source = database();
        isolatePostgres(source);
        migrate(source, V1);
        var jdbc = new JdbcTemplate(source);
        int v1Count = jdbc.queryForObject("SELECT count(*) FROM databasechangelog", Integer.class);
        migrate(source, V2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM databasechangelog", Integer.class))
                .isEqualTo(v1Count + modules.length);
    }

    @Test
    void removesTheObsoleteAccountingGroupAndItsParameters() throws Exception {
        var source = database();
        isolatePostgres(source);
        migrate(source, V1);
        var jdbc = new JdbcTemplate(source);
        jdbc.update("INSERT INTO paramnumber (param_name, param_value) VALUES ('Jour_Debut_Plage_Compta', 3), ('Jour_Fin_Plage_Compta', 4)");

        migrate(source, V2);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM information_schema.columns WHERE table_schema = current_schema() AND lower(table_name) = 'activites' AND lower(column_name) = 'groupe_compta'", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM paramnumber WHERE param_name IN ('Jour_Debut_Plage_Compta', 'Jour_Fin_Plage_Compta')", Integer.class)).isZero();
    }

    @Test
    void removesTheObsoleteFreeTextActivitySchedule() throws Exception {
        var source = database();
        isolatePostgres(source);
        migrate(source, V1);

        migrate(source, V2);

        var jdbc = new JdbcTemplate(source);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM information_schema.columns WHERE table_schema = current_schema() AND lower(table_name) = 'activites' AND lower(column_name) = 'horaire'", Integer.class)).isZero();
    }

    @Test
    void adoptsHistoricalSchemaAndPreservesData() throws Exception {
        var source = database();
        isolatePostgres(source);
        migrate(source, V1);
        var jdbc = new JdbcTemplate(source);
        jdbc.update("INSERT INTO users (id, username, password) VALUES (42, 'historique', 'hash')");
        jdbc.update("INSERT INTO user_role_names (user_id, role_name) VALUES (42, 'ROLE_ADMIN')");
        dropPrimaryKey(jdbc, "user_role_names");
        jdbc.update("""
                INSERT INTO activites (id, groupe, prise_en_charge, autorisation_parentale, certificat_medical,
                    charte_amicale, complete, vie_club)
                VALUES (41, 'ALOD_G', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),
                       (42, 'ALOD_B', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE)
                """);
        jdbc.update("""
                INSERT INTO activites_nm1 (id, groupe)
                VALUES (41, 'ALOD_G'), (42, 'ALOD_B')
                """);
        // Simulate a historical Hibernate schema without Liquibase tracking.
        jdbc.execute("DROP TABLE databasechangelog");
        jdbc.execute("DROP TABLE databasechangeloglock");
        migrate(source, MASTER);
        validateMappings(source);
        assertThat(jdbc.queryForObject("SELECT username FROM users WHERE id = 42", String.class)).isEqualTo("historique");
        assertThat(jdbc.queryForList("SELECT groupe FROM activites ORDER BY id", String.class))
                .containsExactly("NON_COMPETITIVE", "COMPETITION");
        assertThat(jdbc.queryForList("SELECT s.type FROM activites a JOIN sections s ON s.id = a.section_id ORDER BY a.id", String.class))
                .containsExactly("NON_COMPETITIVE", "COMPETITION");
        assertThat(jdbc.queryForList("SELECT groupe FROM activites_nm1 ORDER BY id", String.class))
                .containsExactly("NON_COMPETITIVE", "COMPETITION");
        jdbc.update("INSERT INTO user_role_names (user_id, role_name) VALUES (42, 'ROLE_RESPONSABLE_BOUTIQUE')");
        assertThat(jdbc.queryForList("SELECT role_name FROM user_role_names WHERE user_id = 42 ORDER BY role_name", String.class))
                .containsExactly("ROLE_ADMIN", "ROLE_RESPONSABLE_BOUTIQUE");
        assertThatThrownBy(() -> jdbc.update("INSERT INTO user_role_names (user_id, role_name) VALUES (42, 'INVALID')"))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
    }

    @Test
    void createsV2ShopSchemaAndPreservesDataOnRestart() throws Exception {
        var source = database();
        isolatePostgres(source);
        migrate(source, MASTER);
        var jdbc = new JdbcTemplate(source);
        jdbc.update("""
                INSERT INTO shop_products (name, slug, created_at, updated_at)
                VALUES ('Tee-shirt', 'tee-shirt', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        jdbc.update("""
                INSERT INTO shop_product_variants (product_id, sku, price_amount_cents, price_currency,
                    created_at, updated_at) VALUES (1, 'TS-M', 1500, 'EUR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        String[] statuses = {"COMPLETED", "CANCELLED", "PROCESSING", "PENDING"};
        for (int index = 0; index < statuses.length; index++) {
            int id = index + 1;
            jdbc.update("""
                    INSERT INTO shop_orders (id, order_number, customer_user_id, status, total_amount_cents,
                        total_currency, created_at, updated_at)
                    VALUES (?, ?, 42, ?, 100, 'EUR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """, id, "CMD-" + id, statuses[index]);
            jdbc.update("""
                    INSERT INTO shop_order_items (id, order_id, product_id, product_name, unit_price_amount_cents,
                        unit_price_currency, quantity, line_total_amount_cents, line_total_currency, status, created_at, updated_at)
                    VALUES (?, ?, 1, 'Tee-shirt', 100, 'EUR', 1, 100, 'EUR', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """, id, id, statuses[index]);
        }
        migrate(source, MASTER);
        assertThat(jdbc.queryForList("SELECT status FROM shop_order_items ORDER BY id", String.class))
                .containsExactly("COMPLETED", "CANCELLED", "PROCESSING", "PENDING");
        jdbc.update("UPDATE shop_product_variants SET stock_on_hand = -2, stock_reserved = 3 WHERE sku = 'TS-M'");
        jdbc.update("UPDATE shop_order_items SET status = 'PROCESSING' WHERE id = 4");
        jdbc.update("""
                INSERT INTO shop_supplier_orders (supplier_name, status, created_at, updated_at)
                VALUES ('Fournisseur', 'DRAFT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        jdbc.update("""
                INSERT INTO shop_supplier_order_lines (order_id, variant_id, product_name, sku, quantity,
                    unit_cost_amount_cents, unit_cost_currency, expected_need)
                VALUES (1, 1, 'Tee-shirt', 'TS-M', 5, 800, 'EUR', 5)
                """);
        migrate(source, MASTER);
        validateMappings(source);
        assertThat(jdbc.queryForObject("SELECT status FROM shop_order_items WHERE id = 4", String.class)).isEqualTo("PROCESSING");
        assertThat(jdbc.queryForObject("SELECT name FROM shop_products WHERE id = 1", String.class)).isEqualTo("Tee-shirt");
        assertThat(jdbc.queryForObject("SELECT stock_on_hand FROM shop_product_variants WHERE id = 1", Long.class)).isEqualTo(-2L);
        assertThat(jdbc.queryForObject("SELECT unit_cost_amount_cents FROM shop_supplier_order_lines WHERE order_id = 1", Long.class)).isEqualTo(800L);
    }

    private void isolatePostgres(DriverManagerDataSource source) {
        if (System.getenv("MIGRATION_TEST_URL") != null) {
            String schema = "test_" + UUID.randomUUID().toString().replace("-", "");
            new JdbcTemplate(source).execute("CREATE SCHEMA " + schema);
            source.setUrl(source.getUrl() + "?currentSchema=" + schema);
        }
    }

    private void dropPrimaryKey(JdbcTemplate jdbc, String tableName) {
        String constraintName = jdbc.queryForObject("""
                SELECT constraint_name
                FROM information_schema.table_constraints
                WHERE lower(table_schema) = lower(current_schema())
                  AND lower(table_name) = lower(?)
                  AND constraint_type = 'PRIMARY KEY'
                """, String.class, tableName);
        jdbc.execute("ALTER TABLE " + tableName + " DROP CONSTRAINT \""
                + constraintName.replace("\"", "\"\"") + "\"");
    }
}
