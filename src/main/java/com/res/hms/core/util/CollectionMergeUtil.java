package com.res.hms.core.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CollectionMergeUtil {

  /**
   * Merges an incoming collection of DTOs into an existing collection of Entities.
   *
   * @param existingEntities The current collection from the DB (managed by JPA).
   * @param incomingDtos The new state from the request.
   * @param getEntityId Function to extract ID from Entity.
   * @param getDtoId Function to extract ID from DTO.
   * @param createFn Function to create a new Entity from a DTO.
   * @param updateFn Function to update an existing Entity from a DTO.
   * @param deleteFn Function to handle deletion of an Entity (e.g., removing from parent
   *     collection).
   * @param <E> Entity type.
   * @param <D> DTO type.
   * @param <ID> ID type (e.g., UUID).
   */
  public static <E, D, ID> void merge(
      Collection<E> existingEntities,
      Collection<D> incomingDtos,
      Function<E, ID> getEntityId,
      Function<D, ID> getDtoId,
      Function<D, E> createFn,
      BiConsumer<E, D> updateFn,
      Consumer<E> deleteFn) {

    if (incomingDtos == null) {
      log.debug("Incoming DTO collection is null. Deleting all existing entities.");
      new ArrayList<>(existingEntities).forEach(deleteFn);
      return;
    }

    // 1. Map existing entities by ID for quick lookup
    Map<ID, E> existingMap =
        existingEntities.stream()
            .filter(e -> getEntityId.apply(e) != null)
            .collect(Collectors.toMap(getEntityId, Function.identity()));

    // 2. Identify and handle Deletions (In DB but NOT in Incoming)
    Set<ID> incomingIds =
        incomingDtos.stream().map(getDtoId).filter(Objects::nonNull).collect(Collectors.toSet());

    existingEntities.stream()
        .filter(e -> !incomingIds.contains(getEntityId.apply(e)))
        .collect(Collectors.toList()) // Collect to avoid ConcurrentModificationException
        .forEach(
            e -> {
              log.info("Deleting entity with ID: {}", getEntityId.apply(e));
              deleteFn.accept(e);
            });

    // 3. Identify and handle Creations (No ID) and Updates (Has ID)
    for (D dto : incomingDtos) {
      ID dtoId = getDtoId.apply(dto);
      if (dtoId == null) {
        log.info("Creating new entity from DTO");
        E newEntity = createFn.apply(dto);
        existingEntities.add(newEntity);
      } else {
        E existing = existingMap.get(dtoId);
        if (existing == null) {
          log.error("Update failed. Entity with ID {} not found in existing collection.", dtoId);
          throw new com.res.hms.core.exception.ApiException(
              "NOT_FOUND",
              "Entity with ID " + dtoId + " not found or does not belong to parent",
              org.springframework.http.HttpStatus.NOT_FOUND);
        }
        log.info("Updating existing entity with ID: {}", dtoId);
        updateFn.accept(existing, dto);
      }
    }
  }
}
