-- V23__sync_temperature_ranges_to_fahrenheit.sql

-- Update existing temperature ranges to Fahrenheit standard (95-108)
UPDATE public.vital_range_configs
SET min_value = 95,
    max_value = 108
WHERE vital_type = 'BODY_TEMPERATURE';
