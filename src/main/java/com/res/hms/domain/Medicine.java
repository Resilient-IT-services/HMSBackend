package com.res.hms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "medicine")
public class Medicine extends HospitalScopedEntity {

    @Column(nullable = false)
    private String name;

    private String composition;

    private String manufacturer;
}
