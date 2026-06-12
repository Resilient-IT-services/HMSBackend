package com.res.hms.core.util;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.jpa.domain.Specification;

public class SpecificationBuilder<T> {

  private final Map<String, String> filters;
  private final String search;
  private final List<String> searchColumns;
  private final List<String> ignoredParams;
  private boolean useDistinct = false;

  public SpecificationBuilder(
      Map<String, String> filters, String search, List<String> searchColumns) {
    this.filters = filters;
    this.search = search;
    this.searchColumns = searchColumns;
    this.ignoredParams = List.of("page", "size", "sort", "search", "includeDeleted");
  }

  public SpecificationBuilder<T> useDistinct(boolean useDistinct) {
    this.useDistinct = useDistinct;
    return this;
  }

  public Specification<T> build() {
    return (root, query, cb) -> {
      if (useDistinct
          && query.getResultType() != Long.class
          && query.getResultType() != long.class) {
        query.distinct(true);
      }

      Map<String, Join<?, ?>> joins = new HashMap<>();

      Predicate searchPredicate = createSearchPredicate(root, cb, joins);
      Predicate filterPredicate = createFilterPredicate(root, cb, joins);

      if (searchPredicate != null && filterPredicate != null) {
        return cb.and(searchPredicate, filterPredicate);
      } else if (searchPredicate != null) {
        return searchPredicate;
      } else {
        return filterPredicate;
      }
    };
  }

  private Predicate createSearchPredicate(
      Root<T> root, CriteriaBuilder cb, Map<String, Join<?, ?>> joins) {
    if (search == null || search.isBlank() || searchColumns == null || searchColumns.isEmpty()) {
      return null;
    }
    String pattern = "%" + search.toLowerCase().trim() + "%";
    List<Predicate> predicates = new ArrayList<>();
    for (String column : searchColumns) {
      predicates.add(cb.like(cb.lower(getPath(root, column, joins).as(String.class)), pattern));
    }
    return cb.or(predicates.toArray(new Predicate[0]));
  }

  @SuppressWarnings("unchecked")
  private Predicate createFilterPredicate(
      Root<T> root, CriteriaBuilder cb, Map<String, Join<?, ?>> joins) {
    List<Predicate> predicates = new ArrayList<>();
    for (Map.Entry<String, String> entry : filters.entrySet()) {
      if (ignoredParams.contains(entry.getKey())
          || entry.getValue() == null
          || entry.getValue().isBlank()) {
        continue;
      }

      String key = entry.getKey();
      String value = entry.getValue();
      Path<?> path;
      try {
        path = getPath(root, key, joins);
      } catch (Exception e) {
        // Skip parameters that don't match any property
        continue;
      }

      if (path.getJavaType().equals(Boolean.class) || path.getJavaType().equals(boolean.class)) {
        predicates.add(cb.equal(path, Boolean.valueOf(value)));
      } else if (path.getJavaType().isEnum()
          || path.getModel() != null && path.getModel().getBindableJavaType().isEnum()) {
        try {
          Class<? extends Enum> enumType =
              (Class<? extends Enum>)
                  (path.getJavaType().isEnum()
                      ? path.getJavaType()
                      : path.getModel().getBindableJavaType());
          predicates.add(cb.equal(path, Enum.valueOf(enumType, value.toUpperCase())));
        } catch (IllegalArgumentException e) {
          // Skip invalid enum values instead of throwing exception
          continue;
        }
      } else if (path.getJavaType().equals(String.class)) {
        predicates.add(
            cb.like(cb.lower(path.as(String.class)), "%" + value.toLowerCase().trim() + "%"));
      } else {
        predicates.add(cb.equal(path.as(String.class), value));
      }
    }
    return predicates.isEmpty() ? null : cb.and(predicates.toArray(new Predicate[0]));
  }

  private Path<?> getPath(Root<T> root, String attributeName, Map<String, Join<?, ?>> joins) {
    String[] parts = attributeName.split("\\.");
    if (parts.length == 1) {
      return root.get(parts[0]);
    }

    From<?, ?> from = root;
    StringBuilder pathSoFar = new StringBuilder();
    for (int i = 0; i < parts.length - 1; i++) {
      if (i > 0) {
        pathSoFar.append(".");
      }
      pathSoFar.append(parts[i]);
      String key = pathSoFar.toString();
      if (joins.containsKey(key)) {
        from = joins.get(key);
      } else {
        Join<?, ?> join = from.join(parts[i], JoinType.LEFT);
        joins.put(key, join);
        from = join;
      }
    }
    return from.get(parts[parts.length - 1]);
  }
}
