package com.res.hms.lab.domain;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "lab_sample_barcode", schema = "lab")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabSampleBarcode {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "sample_id")
    private UUID sampleId;

    @Column(name = "barcode_value")
    private String barcodeValue;

    @Column(name = "qr_code_value")
    private String qrCodeValue;
}
