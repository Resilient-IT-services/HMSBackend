package com.res.hms.common.util;

import com.res.hms.common.exception.ApiException;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ResourceLookup {

  public <T, K> T findRequired(
      K id, Long hospitalId, BiFunction<K, Long, Optional<T>> finder, String resourceName) {
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
