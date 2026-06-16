package com.res.hms.pharmacy.infrastructure.repository;

import com.res.hms.core.repository.BaseRepository;
import com.res.hms.pharmacy.domain.PharmacyStock;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PharmacyStockRepository extends BaseRepository<PharmacyStock> {
  List<PharmacyStock> findByHospitalIdOrderByMedicineNameAsc(UUID hospitalId);

  Page<PharmacyStock> findAllByHospitalIdAndQuantityLessThan(
      UUID hospitalId, Integer threshold, Pageable pageable);

  @Query(
      """
      SELECT s FROM PharmacyStock s
      JOIN FETCH s.medicine m
      WHERE s.hospitalId = :hospitalId
        AND s.quantity > 0
        AND s.quantity <= m.thresholdQuantity
      ORDER BY s.medicine.name ASC
      """)
  Page<PharmacyStock> findLowStock(@Param("hospitalId") UUID hospitalId, Pageable pageable);

  @Override
  Optional<PharmacyStock> findByIdAndHospitalId(UUID id, UUID hospitalId);

  Optional<PharmacyStock> findByHospitalIdAndBatchNumber(UUID hospitalId, String batchNumber);

  Optional<PharmacyStock> findByHospitalIdAndMedicineIdAndBatchNumber(
      UUID hospitalId, UUID medicineId, String batchNumber);

  Page<PharmacyStock> findAllByHospitalIdAndMedicineId(
      UUID hospitalId, UUID medicineId, Pageable pageable);

  @Query(
      """
      SELECT s FROM PharmacyStock s
      JOIN FETCH s.medicine
      WHERE s.hospitalId = :hospitalId
      ORDER BY s.medicine.name, s.expiryDate ASC
      """)
  Page<PharmacyStock> findDetailedStock(@Param("hospitalId") UUID hospitalId, Pageable pageable);

  @Query(
      """
      SELECT s FROM PharmacyStock s
      JOIN FETCH s.medicine
      WHERE s.hospitalId = :hospitalId
        AND (LOWER(s.medicine.name) LIKE LOWER(CONCAT('%', :search, '%'))
             OR LOWER(s.batchNumber) LIKE LOWER(CONCAT('%', :search, '%')))
      ORDER BY s.medicine.name, s.expiryDate ASC
      """)
  Page<PharmacyStock> searchDetailedStock(
      @Param("hospitalId") UUID hospitalId, @Param("search") String search, Pageable pageable);

  @Query(
      """
      SELECT s FROM PharmacyStock s
      JOIN FETCH s.medicine
      WHERE s.hospitalId = :hospitalId
        AND s.quantity > 0
        AND (s.expiryDate IS NULL OR s.expiryDate >= :today)
        AND s.medicine.id = :medicineId
      ORDER BY s.expiryDate ASC, s.createdAt ASC
      """)
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  List<PharmacyStock> findDispensableBatchesForUpdate(
      @Param("hospitalId") UUID hospitalId,
      @Param("medicineId") UUID medicineId,
      @Param("today") LocalDate today);

  @Query(
      """
      SELECT COALESCE(SUM(s.quantity), 0) FROM PharmacyStock s
      WHERE s.hospitalId = :hospitalId
        AND s.medicine.id = :medicineId
        AND s.quantity > 0
        AND (s.expiryDate IS NULL OR s.expiryDate >= :today)
      """)
  Long availableQuantity(
      @Param("hospitalId") UUID hospitalId,
      @Param("medicineId") UUID medicineId,
      @Param("today") LocalDate today);

  @Query(
      """
      SELECT s.medicine.id, COALESCE(SUM(s.quantity), 0) FROM PharmacyStock s
      WHERE s.hospitalId = :hospitalId
        AND s.medicine.id IN :medicineIds
        AND s.quantity > 0
        AND (s.expiryDate IS NULL OR s.expiryDate >= :today)
      GROUP BY s.medicine.id
      """)
  List<Object[]> availableQuantities(
      @Param("hospitalId") UUID hospitalId,
      @Param("medicineIds") List<UUID> medicineIds,
      @Param("today") LocalDate today);

  @Query(
      """
      SELECT s FROM PharmacyStock s
      JOIN FETCH s.medicine
      WHERE s.hospitalId = :hospitalId
        AND s.quantity > 0
        AND s.expiryDate BETWEEN :today AND :until
      ORDER BY s.expiryDate ASC
      """)
  List<PharmacyStock> findExpiringBatches(
      @Param("hospitalId") UUID hospitalId,
      @Param("today") LocalDate today,
      @Param("until") LocalDate until);

  long countByHospitalId(UUID hospitalId);

  @Query(
      "SELECT COUNT(DISTINCT s.medicine.id) FROM PharmacyStock s WHERE s.hospitalId = :hospitalId")
  long countUniqueMedicines(@Param("hospitalId") UUID hospitalId);

  @Query(
      """
      SELECT COUNT(s) FROM PharmacyStock s
      JOIN s.medicine m
      WHERE s.hospitalId = :hospitalId
        AND s.quantity > 0
        AND s.quantity <= m.thresholdQuantity
      """)
  long countLowStock(@Param("hospitalId") UUID hospitalId);

  @Query(
      """
      SELECT COUNT(s) FROM PharmacyStock s
      WHERE s.hospitalId = :hospitalId
        AND s.quantity > 0
        AND s.expiryDate BETWEEN :today AND :until
      """)
  long countExpiringSoon(
      @Param("hospitalId") UUID hospitalId,
      @Param("today") LocalDate today,
      @Param("until") LocalDate until);
}
