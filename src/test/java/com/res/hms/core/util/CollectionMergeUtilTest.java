package com.res.hms.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CollectionMergeUtilTest {

  static class Item {
    private UUID id;
    private String val;

    Item(UUID id, String val) {
      this.id = id;
      this.val = val;
    }

    UUID getId() {
      return id;
    }

    String getVal() {
      return val;
    }

    void setVal(String val) {
      this.val = val;
    }
  }

  static class Dto {
    private UUID id;
    private String val;

    Dto(UUID id, String val) {
      this.id = id;
      this.val = val;
    }

    UUID getId() {
      return id;
    }

    String getVal() {
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

    assertEquals(2, existing.size());
    assertEquals("new", existing.get(0).getVal());
    assertEquals("added", existing.get(1).getVal());
    assertTrue(removed.isEmpty());
  }

  @Test
  void testMerge_WithRemoval() {
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

    assertEquals(1, existing.size());
    assertEquals(1, removed.size());
    assertEquals(id2, removed.get(0).getId());
  }
}
