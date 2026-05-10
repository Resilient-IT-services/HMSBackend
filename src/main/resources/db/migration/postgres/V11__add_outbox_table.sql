-- V11__add_outbox_table.sql
CREATE TABLE public.outbox_event (
    id uuid NOT NULL,
    aggregate_type varchar(255) NOT NULL,
    aggregate_id uuid NOT NULL,
    event_type varchar(255) NOT NULL,
    payload jsonb NOT NULL,
    created_at timestamp NOT NULL,
    processed_at timestamp NULL,
    CONSTRAINT outbox_event_pkey PRIMARY KEY (id)
);

CREATE INDEX idx_outbox_unprocessed ON public.outbox_event (created_at) WHERE processed_at IS NULL;
