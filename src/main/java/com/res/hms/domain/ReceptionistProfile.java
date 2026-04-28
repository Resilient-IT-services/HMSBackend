package com.res.hms.domain;

import com.res.hms.user.dto.ReceptionistFluency;
import io.hypersistence.utils.hibernate.type.json.JsonType;
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
import org.hibernate.annotations.Type;

@Getter
@Setter
@Entity
@Table(name = "receptionist_profile")
public class ReceptionistProfile extends AuditableEntity {

  @Id
  @Column(name = "id")
  private UUID id;

  @OneToOne(fetch = FetchType.LAZY)
  @MapsId
  @JoinColumn(name = "id")
  private UserAccount user;

  @Type(JsonType.class)
  @Column(name = "languages_spoken", columnDefinition = "jsonb")
  private ReceptionistFluency languagesSpoken;

  @Type(JsonType.class)
  @Column(name = "skills_json", columnDefinition = "jsonb")
  private Object skills;

  @Column(name = "is_insurance_specialist")
  private boolean insuranceSpecialist = false;
}
