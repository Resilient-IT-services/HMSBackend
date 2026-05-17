-- V21__make_vital_ranges_global.sql

-- 1. Deduplicate existing configs (keep only one per vital type)
DELETE FROM public.vital_range_configs
WHERE id IN (
    SELECT id
    FROM (
        SELECT id,
               ROW_NUMBER() OVER (PARTITION BY vital_type ORDER BY created_at ASC) as r_num
        FROM public.vital_range_configs
    ) t
    WHERE r_num > 1
);

-- 2. Drop constraints and hospital_id column
ALTER TABLE public.vital_range_configs DROP CONSTRAINT IF EXISTS uk_vital_hospital;
ALTER TABLE public.vital_range_configs DROP CONSTRAINT IF EXISTS fk_vrc_hospital;
ALTER TABLE public.vital_range_configs DROP COLUMN hospital_id;

-- 3. Add global unique constraint on vital_type
ALTER TABLE public.vital_range_configs ADD CONSTRAINT uk_vital_type UNIQUE (vital_type);
