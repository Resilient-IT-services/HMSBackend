-- V1014__admin_dashboard_metadata.sql
-- Adding metadata columns to support comprehensive Admin Dashboard analytics

-- 1. Update Hospital table with subscription and city details
ALTER TABLE core.hospital 
ADD COLUMN IF NOT EXISTS subscription_plan VARCHAR(50) DEFAULT 'BASIC',
ADD COLUMN IF NOT EXISTS subscription_expiry_date TIMESTAMP,
ADD COLUMN IF NOT EXISTS latitude NUMERIC(10, 8),
ADD COLUMN IF NOT EXISTS longitude NUMERIC(11, 8);

-- 2. Update User Account to track active status
ALTER TABLE iam.user_account
ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMP;

-- 3. Create a platform alerts table if not exists (modular schema 'platform' already exists from V1001)
CREATE TABLE IF NOT EXISTS platform.system_alerts (
    id UUID PRIMARY KEY,
    hospital_id UUID REFERENCES core.hospital(id),
    alert_type VARCHAR(100) NOT NULL,
    severity VARCHAR(20) NOT NULL, -- CRITICAL, WARNING, INFO
    message TEXT NOT NULL,
    is_resolved BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP
);

-- 4. Seed some sample data for top-performing hospitals logic if needed
-- (Assuming hospitals and users already exist from baseline)
