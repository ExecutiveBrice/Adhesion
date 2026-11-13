package com.wild.corp.adhesion.shop.migration;

import com.wild.corp.adhesion.models.ERole;
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

import static org.assertj.core.api.Assertions.assertThat;

class ShopMigrationTest {
    private SingleConnectionDataSource embeddedDatabase;

    @AfterEach
    void closeDatabase() {
        if (embeddedDatabase != null) embeddedDatabase.destroy();
    }

    @Test
    void createsShopAlongsideHistoricalTablesAndPreservesDataOnRestart() throws Exception {
        var source = database();
        var jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE users (id bigint PRIMARY KEY, username varchar(255))");
        jdbc.execute("CREATE TABLE user_role_names (user_id bigint, role_name varchar(32))");
        jdbc.update("INSERT INTO users VALUES (42, 'historique')");
        jdbc.update("INSERT INTO user_role_names VALUES (42, 'ROLE_REFERENT')");
        if (System.getenv("MIGRATION_TEST_URL") != null) {
            jdbc.execute("ALTER TABLE user_role_names ADD CONSTRAINT historical_roles CHECK (role_name IN ('ROLE_USER', 'ROLE_REFERENT'))");
        }

        migrate(source);
        validateShopMappings(source);
        jdbc.update("""
                INSERT INTO shop_products (name, slug, created_at, updated_at)
                VALUES ('Tee-shirt', 'tee-shirt', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        jdbc.update("""
                INSERT INTO shop_product_variants (product_id, sku, price_amount_cents, price_currency,
                    stock_tracked, stock_on_hand, stock_reserved, created_at, updated_at)
                VALUES (1, 'TS-M', 1500, 'EUR', TRUE, -2, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        jdbc.update("INSERT INTO user_role_names VALUES (42, 'ROLE_RESPONSABLE_BOUTIQUE')");
        int changesets = jdbc.queryForObject("SELECT count(*) FROM databasechangelog", Integer.class);
        Long number = jdbc.queryForObject("SELECT nextval('shop_order_number_seq')", Long.class);

        migrate(source);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM databasechangelog", Integer.class)).isEqualTo(changesets);
        assertThat(jdbc.queryForObject("SELECT username FROM users WHERE id = 42", String.class)).isEqualTo("historique");
        assertThat(jdbc.queryForList("SELECT role_name FROM user_role_names ORDER BY role_name", String.class))
                .containsExactly("ROLE_REFERENT", "ROLE_RESPONSABLE_BOUTIQUE");
        assertThat(jdbc.queryForObject("SELECT stock_on_hand FROM shop_product_variants", Long.class)).isEqualTo(-2L);
        assertThat(jdbc.queryForObject("SELECT nextval('shop_order_number_seq')", Long.class)).isEqualTo(number + 1);
        for (ERole role : ERole.values()) {
            jdbc.update("INSERT INTO user_role_names VALUES (42, ?)", role.name());
        }
    }

    @Test
    void upgradesExistingProductsWithoutChangingTheirMainImage() throws Exception {
        var source = database();
        var jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE users (id bigint PRIMARY KEY, username varchar(255))");
        jdbc.execute("CREATE TABLE user_role_names (user_id bigint, role_name varchar(32))");
        migrate(source, "db/changelog/v2/shop.xml");
        jdbc.update("""
                INSERT INTO shop_products (name, slug, image_url, created_at, updated_at)
                VALUES ('Photos', 'photos', '/main.jpg', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);

        migrate(source);
        validateShopMappings(source);
        assertThat(jdbc.queryForObject("SELECT image_url FROM shop_products", String.class)).isEqualTo("/main.jpg");
        assertThat(jdbc.queryForObject("SELECT additional_image_url_1 FROM shop_products", String.class)).isNull();
        assertThat(jdbc.queryForObject("SELECT additional_image_url_2 FROM shop_products", String.class)).isNull();
        jdbc.update("UPDATE shop_products SET additional_image_url_1 = '/front.jpg', additional_image_url_2 = '/back.jpg'");
        migrate(source);
        assertThat(jdbc.queryForObject("SELECT additional_image_url_2 FROM shop_products", String.class)).isEqualTo("/back.jpg");
    }

    private DriverManagerDataSource database() {
        String postgresUrl = System.getenv("MIGRATION_TEST_URL");
        if (postgresUrl == null) {
            // Keep the creating session alive for H2 CHECK expressions.
            embeddedDatabase = new SingleConnectionDataSource("jdbc:h2:mem:shop_" + UUID.randomUUID()
                    + ";MODE=PostgreSQL", "sa", "", true);
            return embeddedDatabase;
        }
        var source = new DriverManagerDataSource(postgresUrl,
                System.getenv("MIGRATION_TEST_USER"), System.getenv("MIGRATION_TEST_PASSWORD"));
        String schema = "shop_test_" + UUID.randomUUID().toString().replace("-", "");
        new JdbcTemplate(source).execute("CREATE SCHEMA " + schema);
        source.setUrl(postgresUrl + (postgresUrl.contains("?") ? "&" : "?") + "currentSchema=" + schema);
        return source;
    }

    private void migrate(DriverManagerDataSource source) throws Exception {
        migrate(source, "db/changelog/db.changelog-shop.xml");
    }

    private void migrate(DriverManagerDataSource source, String changelog) throws Exception {
        try (var connection = source.getConnection()) {
            var database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            try (var liquibase = new Liquibase(changelog,
                    new ClassLoaderResourceAccessor(), database)) {
                liquibase.update(new Contexts(), new LabelExpression());
            }
        }
    }

    private void validateShopMappings(DriverManagerDataSource source) throws Exception {
        var settings = new HashMap<String, Object>();
        settings.put("hibernate.connection.datasource", source);
        settings.put("hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl");
        settings.put("hibernate.hbm2ddl.auto", "validate");
        var registry = new StandardServiceRegistryBuilder().applySettings(settings).build();
        try {
            var metadata = new MetadataSources(registry);
            var scanner = new ClassPathScanningCandidateComponentProvider(false);
            scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
            for (var bean : scanner.findCandidateComponents("com.wild.corp.adhesion.shop")) {
                metadata.addAnnotatedClass(Class.forName(bean.getBeanClassName()));
            }
            SchemaManagementToolCoordinator.process(metadata.buildMetadata(), registry, settings, action -> { });
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }
}
