package com.res.hms.hospital.domain;

import com.res.hms.core.domain.BaseEntity;
import com.res.hms.hospital.application.dto.HospitalSettings;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "hospital")
public class Hospital extends BaseEntity {

  @Column(nullable = false)
  private String name;

  @Column(nullable = false, unique = true)
  private String code;

  private String address;

  private String city;

  private String state;

  private String pincode;

  @Column(unique = true)
  private String gstin;

  @Column(name = "registration_no")
  private String registrationNo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private HospitalStatus status;

  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
  @Column(name = "settings", columnDefinition = "jsonb")
  private HospitalSettings settings;

  @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @JoinTable(
      name = "hospital_departments",
      joinColumns = @JoinColumn(name = "hospital_id"),
      inverseJoinColumns = @JoinColumn(name = "department_id"))
  private Set<Department> departments = new HashSet<>();

  @Column(nullable = false)
  private boolean deleted = false;
}
