-- Existing databases may have been initialized by Hibernate while Flyway's
-- Spring Boot auto-configuration was absent. Ensure the custom prerequisite
-- that Hibernate cannot infer from the entity mappings.
CREATE SEQUENCE IF NOT EXISTS shop_order_number_seq START WITH 1 INCREMENT BY 1;
