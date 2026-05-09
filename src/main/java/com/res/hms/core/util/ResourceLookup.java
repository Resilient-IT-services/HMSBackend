package com.res.hms.core.util;

import com.res.hms.core.exception.ApiException;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ResourceLookup {

  public <T, K> T findRequired(
      K id, UUID hospitalId, BiFunction<K, UUID, Optional<T>> finder, String resourceName) {
    return finder
        .apply(id, hospitalId)
        .orElseThrow(
            () -> new ApiException("NOT_FOUND", resourceName + " not found", HttpStatus.NOT_FOUND));
  }

  public <T, K> T findRequired(K id, Function<K, Optional<T>> finder, String resourceName) {
    return finder
        .apply(id)
        .orElseThrow(
            () -> new ApiException("NOT_FOUND", resourceName + " not found", HttpStatus.NOT_FOUND));
  }
}
