package com.res.hms.lab.domain;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "lab_report_template", schema = "lab")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabReportTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "test_id")
    private UUID testId;

    @Column(name = "template_name")
    private String templateName;

    @Type(io.hypersistence.utils.hibernate.type.json.JsonType.class)
    @Column(name = "template_json", columnDefinition = "jsonb")
    private String templateJson;
}
