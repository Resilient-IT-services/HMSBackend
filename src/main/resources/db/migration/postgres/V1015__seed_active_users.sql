-- V1015__seed_active_users.sql
-- Seed last_login_at for existing users so the "Active Now" KPI card shows data.
-- In a real production environment, the Auth login flow will update this column.

UPDATE iam.user_account
SET last_login_at = CURRENT_TIMESTAMP
WHERE id IN (
    SELECT id FROM iam.user_account LIMIT 15
);
