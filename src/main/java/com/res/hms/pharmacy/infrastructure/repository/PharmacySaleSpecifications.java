package com.res.hms.pharmacy.infrastructure.repository;

import com.res.hms.pharmacy.domain.PharmacySale;
import com.res.hms.pharmacy.domain.SaleType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class PharmacySaleSpecifications {

  private PharmacySaleSpecifications() {
    // Utility class
  }

  public static Specification<PharmacySale> withFilters(
      UUID hospitalId, String search, Map<String, String> filters) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      predicates.add(cb.equal(root.get("hospitalId"), hospitalId));

      if (filters.containsKey("saleType")) {
        try {
          predicates.add(
              cb.equal(
                  root.get("saleType"),
                  SaleType.valueOf(filters.get("saleType").toUpperCase(Locale.ROOT))));
        } catch (IllegalArgumentException ignored) {
        }
      }

      if (search != null && !search.isBlank()) {
        String pattern = "%" + search.toLowerCase(Locale.ROOT).trim() + "%";
        List<Predicate> searchOr = new ArrayList<>();
        searchOr.add(cb.like(cb.lower(root.get("saleNumber")), pattern));
        searchOr.add(cb.like(cb.lower(root.get("customerName")), pattern));
        searchOr.add(cb.like(cb.lower(root.get("customerPhone")), pattern));
        searchOr.add(cb.like(cb.lower(root.get("externalDoctorName")), pattern));

        try {
          UUID searchUuid = UUID.fromString(search.trim());
          searchOr.add(cb.equal(root.get("prescriptionId"), searchUuid));
        } catch (IllegalArgumentException ignored) {
        }

        predicates.add(cb.or(searchOr.toArray(new Predicate[0])));
      }

      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }
}
