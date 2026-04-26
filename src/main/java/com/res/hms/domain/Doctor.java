package com.res.hms.domain;

import com.res.hms.doctor.dto.DoctorEducation;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

@Getter
@Setter
@Entity
@Table(name = "doctor")
public class Doctor extends BaseEntity {

  @OneToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "user_id", unique = true)
  private UserAccount user;

  @Column(nullable = false)
  private String specialization;

  @Column(name = "license_number", unique = true)
  private String licenseNumber;

  @Column(columnDefinition = "text")
  private String bio;

  @Type(JsonType.class)
  @Column(name = "education_json", columnDefinition = "jsonb")
  private DoctorEducation education;

  @OneToMany(mappedBy = "doctor", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<DoctorAvailabilityException> availabilityExceptions = new ArrayList<>();

  public String getFullName() {

    return user != null ? user.getFullName() : null;
  }
}
