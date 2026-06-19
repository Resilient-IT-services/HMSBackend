-- Backfill prescription_number for all existing prescriptions that don't have one.
-- Safe to re-run: WHERE clause only targets NULL rows.
UPDATE clinical.prescription p
SET prescription_number =
    UPPER(REGEXP_REPLACE(TRIM(h.name), '(\S)\S*(\s+|$)', '\1', 'g'))
        || '-' || TO_CHAR(p.created_at AT TIME ZONE 'Asia/Kolkata', 'YY')
        || '-' || LPAD(NEXTVAL('prescription_number_seq')::TEXT, 5, '0')
FROM core.hospital h
WHERE h.id  = p.hospital_id
  AND p.prescription_number IS NULL;

-- GIN trigram index for fast ILIKE search on prescription_number.
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_prescription_number_trgm
    ON clinical.prescription USING gin (prescription_number gin_trgm_ops);
