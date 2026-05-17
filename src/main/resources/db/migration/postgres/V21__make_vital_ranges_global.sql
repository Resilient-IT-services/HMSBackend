-- V21__make_vital_ranges_global.sql

-- 1. Deduplicate existing configs (keep only one per vital type)
DELETE FROM public.vital_range_configs
WHERE id NOT IN (
    SELECT MIN(id)
    FROM public.vital_range_configs
    GROUP BY vital_type
);

-- 2. Drop constraints and hospital_id column
ALTER TABLE public.vital_range_configs DROP CONSTRAINT IF EXISTS uk_vital_hospital;
ALTER TABLE public.vital_range_configs DROP CONSTRAINT IF EXISTS fk_vrc_hospital;
ALTER TABLE public.vital_range_configs DROP COLUMN hospital_id;

-- 3. Add global unique constraint on vital_type
ALTER TABLE public.vital_range_configs ADD CONSTRAINT uk_vital_type UNIQUE (vital_type);
