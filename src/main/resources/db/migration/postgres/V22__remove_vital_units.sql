-- V22__remove_vital_units.sql

-- 1. Remove unit columns from vitals table
ALTER TABLE public.vitals DROP COLUMN IF EXISTS temperature_unit;
ALTER TABLE public.vitals DROP COLUMN IF EXISTS weight_unit;

-- 2. Remove unit column from vital_range_configs table
ALTER TABLE public.vital_range_configs DROP COLUMN IF EXISTS unit;
