package com.res.hms.iam.domain;

import com.res.hms.core.domain.AuditableEntity;
import com.res.hms.iam.application.dto.DoctorEducation;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "doctor_profile")
public class DoctorProfile extends AuditableEntity {

  @Id
  @Column(name = "id")
  private UUID id;

  @OneToOne(fetch = FetchType.EAGER)
  @MapsId
  @JoinColumn(name = "id")
  private UserAccount user;

  @ManyToMany(
      fetch = FetchType.EAGER,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @JoinTable(
      name = "doctor_specialization",
      joinColumns = @JoinColumn(name = "doctor_profile_id"),
      inverseJoinColumns = @JoinColumn(name = "specialization_id"))
  private Set<Specialization> specializations = new HashSet<>();

  @Column(name = "license_number", unique = true, nullable = true)
  private String licenseNumber;

  @Column(name = "registration_number")
  private String registrationNumber;

  @Column(name = "experience_years")
  private Integer experienceYears;

  @Column(columnDefinition = "text")
  private String bio;

  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
  @Column(name = "education_json", columnDefinition = "jsonb")
  private DoctorEducation education;

  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
  @Column(name = "awards_json", columnDefinition = "jsonb")
  private java.util.List<String> awards;

  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
  @Column(name = "languages_json", columnDefinition = "jsonb")
  private java.util.List<String> languages;

  @Column(name = "signature_url")
  private String signature;

  @Column(name = "consultation_fee")
  private java.math.BigDecimal consultationFee;

  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
  @Column(name = "metadata_json", columnDefinition = "jsonb")
  private java.util.Map<String, Object> metadata;

  public String getFullName() {
    return user != null ? user.getFullName() : null;
  }
}
