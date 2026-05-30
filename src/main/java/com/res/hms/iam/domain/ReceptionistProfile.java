package com.res.hms.iam.domain;

import com.res.hms.core.domain.AuditableEntity;
import com.res.hms.iam.application.dto.ReceptionistFluency;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "receptionist_profile", schema = "staff")
public class ReceptionistProfile extends AuditableEntity {

  @Id
  @Column(name = "id")
  private UUID id;

  @OneToOne(fetch = FetchType.LAZY)
  @MapsId
  @JoinColumn(name = "id")
  private UserAccount user;

  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
  @Column(name = "languages_spoken_json", columnDefinition = "jsonb")
  private ReceptionistFluency languagesSpoken;

  @Column(name = "is_insurance_specialist")
  private boolean insuranceSpecialist = false;
}
