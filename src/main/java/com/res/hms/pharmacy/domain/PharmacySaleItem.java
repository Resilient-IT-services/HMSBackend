package com.res.hms.pharmacy.domain;

import com.res.hms.core.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "pharmacy_sale_item", schema = "pharmacy")
public class PharmacySaleItem extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "sale_id", nullable = false)
  private PharmacySale sale;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "medicine_id", nullable = false)
  private Medicine medicine;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "stock_id", nullable = false)
  private PharmacyStock stock;

  @Column(nullable = false)
  private Integer quantity;

  @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
  private BigDecimal unitPrice = BigDecimal.ZERO;

  @Column(name = "discount_amount", precision = 12, scale = 2)
  private BigDecimal discountAmount = BigDecimal.ZERO;

  @Column(name = "tax_amount", precision = 12, scale = 2)
  private BigDecimal taxAmount = BigDecimal.ZERO;

  @Column(name = "taxable_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal taxableAmount = BigDecimal.ZERO;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal subtotal = BigDecimal.ZERO;

  @Column(name = "net_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal netAmount = BigDecimal.ZERO;

  @jakarta.persistence.OneToMany(mappedBy = "pharmacySaleItem", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
  private java.util.List<PharmacySaleItemTax> taxes = new java.util.ArrayList<>();
}
