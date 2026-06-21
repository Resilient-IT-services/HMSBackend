package com.res.hms.ipd.domain;

import com.res.hms.core.domain.HospitalScopedEntity;
import com.res.hms.iam.domain.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "bed_transfer_history", schema = "ipd")
public class BedTransferHistory extends HospitalScopedEntity {

  @Column(name = "admission_id", nullable = false)
  private UUID admissionId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "admission_id", insertable = false, updatable = false)
  private Admission admission;

  @Column(name = "from_bed_id", nullable = false)
  private UUID fromBedId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "from_bed_id", insertable = false, updatable = false)
  private Bed fromBed;

  @Column(name = "to_bed_id", nullable = false)
  private UUID toBedId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "to_bed_id", insertable = false, updatable = false)
  private Bed toBed;

  @Column(name = "transfer_reason")
  private String transferReason;

  private String remarks;

  @Column(name = "transferred_by", nullable = false)
  private UUID transferredBy;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "transferred_by", insertable = false, updatable = false)
  private UserAccount transferredByUser;

  @Column(name = "transferred_at", nullable = false)
  private Instant transferredAt = Instant.now();
}
