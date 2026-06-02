package com.res.hms.lab.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "lab_inventory_transaction", schema = "lab")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabInventoryTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "item_id")
    private UUID itemId;

    @Column(name = "transaction_type")
    private String transactionType;

    @Column(name = "quantity")
    private BigDecimal quantity;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(name = "transaction_time")
    private Instant transactionTime;
}
