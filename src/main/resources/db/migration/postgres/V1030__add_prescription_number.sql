ALTER TABLE clinical.prescription
    ADD COLUMN prescription_number VARCHAR(50) UNIQUE;

CREATE SEQUENCE IF NOT EXISTS public.prescription_number_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
