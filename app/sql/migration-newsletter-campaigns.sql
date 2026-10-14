-- Optional manual migration when Hibernate schema updates are disabled (PostgreSQL).
-- Existing transactional history retains NULL campaign fields.
ALTER TABLE historique ADD COLUMN IF NOT EXISTS campaign_id BIGINT;
ALTER TABLE historique ADD COLUMN IF NOT EXISTS campaign_status VARCHAR(255);
ALTER TABLE historique ADD COLUMN IF NOT EXISTS brevo_status VARCHAR(255);
CREATE UNIQUE INDEX IF NOT EXISTS historique_campaign_id_unique ON historique (campaign_id);
