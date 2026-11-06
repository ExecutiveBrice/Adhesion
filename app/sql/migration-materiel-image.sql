-- Facultatif si Hibernate applique déjà spring.jpa.hibernate.ddl-auto=update.
ALTER TABLE materiel ADD COLUMN IF NOT EXISTS image text;
