package com.res.hms.ipd.domain;

import com.res.hms.core.domain.HospitalScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "bed", schema = "ipd")
public class Bed extends HospitalScopedEntity {

  @Column(name = "ward_id", nullable = false)
  private UUID wardId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ward_id", insertable = false, updatable = false)
  private Ward ward;

  @Column(name = "bed_number", nullable = false)
  private String bedNumber;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private BedStatus status = BedStatus.AVAILABLE;

  @Column(nullable = false)
  private boolean active = true;
}
