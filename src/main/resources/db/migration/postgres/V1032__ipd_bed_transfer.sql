-- V1032__ipd_bed_transfer.sql
CREATE TABLE ipd.bed_transfer_history (
    id UUID PRIMARY KEY,
    hospital_id UUID NOT NULL,
    admission_id UUID NOT NULL,
    from_bed_id UUID NOT NULL,
    to_bed_id UUID NOT NULL,
    transfer_reason VARCHAR(255),
    remarks VARCHAR(255),
    transferred_by UUID NOT NULL,
    transferred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bed_transfer_history_admission FOREIGN KEY (admission_id) REFERENCES ipd.admission(id),
    CONSTRAINT fk_bed_transfer_history_from_bed FOREIGN KEY (from_bed_id) REFERENCES ipd.bed(id),
    CONSTRAINT fk_bed_transfer_history_to_bed FOREIGN KEY (to_bed_id) REFERENCES ipd.bed(id),
    CONSTRAINT fk_bed_transfer_history_transferred_by FOREIGN KEY (transferred_by) REFERENCES iam.user_account(id)
);
