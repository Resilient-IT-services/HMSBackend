package com.res.hms.core.util;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CollectionMergeUtilTest {

  static class Item {
    private final UUID id;
    private String val;

    /* package */ Item(UUID id, String val) {
      this.id = id;
      this.val = val;
    }

    /* package */ UUID getId() {
      return id;
    }

    /* package */ String getVal() {
      return val;
    }

    /* package */ void setVal(String val) {
      this.val = val;
    }
  }

  static class Dto {
    private final UUID id;
    private final String val;

    /* package */ Dto(UUID id, String val) {
      this.id = id;
      this.val = val;
    }

    /* package */ UUID getId() {
      return id;
    }

    /* package */ String getVal() {
      return val;
    }
  }

  @Test
  void testMerge() {
    UUID id1 = UUID.randomUUID();

    List<Item> existing = new ArrayList<>(List.of(new Item(id1, "old")));
    List<Dto> incoming = List.of(new Dto(id1, "new"), new Dto(null, "added"));
    List<Item> removed = new ArrayList<>();

    CollectionMergeUtil.merge(
        existing,
        incoming,
        Item::getId,
        Dto::getId,
        dto -> new Item(dto.getId(), dto.getVal()),
        (item, dto) -> item.setVal(dto.getVal()),
        removed::add);

    assertAll(
        "Basic merge assertions",
        () -> assertEquals(2, existing.size(), "Should have 2 items"),
        () -> assertEquals("new", existing.get(0).getVal(), "First item should be updated"),
        () -> assertEquals("added", existing.get(1).getVal(), "Second item should be added"),
        () -> assertTrue(removed.isEmpty(), "No items should be removed"));
  }

  @Test
  void testMergeWithRemoval() {
    UUID id1 = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();

    List<Item> existing = new ArrayList<>(List.of(new Item(id1, "old"), new Item(id2, "gone")));
    List<Dto> incoming = List.of(new Dto(id1, "stay"));
    List<Item> removed = new ArrayList<>();

    CollectionMergeUtil.merge(
        existing,
        incoming,
        Item::getId,
        Dto::getId,
        dto -> new Item(dto.getId(), dto.getVal()),
        (item, dto) -> item.setVal(dto.getVal()),
        i -> {
          existing.remove(i);
          removed.add(i);
        });

    assertAll(
        "Merge with removal assertions",
        () -> assertEquals(1, existing.size(), "Should have 1 item left"),
        () -> assertEquals(1, removed.size(), "Should have 1 item removed"),
        () -> assertEquals(id2, removed.get(0).getId(), "Wrong item removed"));
  }
}
