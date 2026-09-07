package com.fulfilment.application.monolith.warehouses.domain.validator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class WarehouseValidatorTest {

  private InMemoryWarehouseStore warehouseStore;
  private FakeLocationResolver locationResolver;
  private WarehouseValidator validator;

  static class InMemoryWarehouseStore implements WarehouseStore {
    final List<Warehouse> warehouses = new ArrayList<>();
    long seq = 1;

    @Override
    public List<Warehouse> getAll() {
      return warehouses.stream().filter(w -> w.archivedAt == null).toList();
    }

    @Override
    public void create(Warehouse warehouse) {
      if (warehouse.id == null) {
        warehouse.id = seq++;
      }
      warehouses.add(warehouse);
    }

    @Override
    public void update(Warehouse warehouse) {}

    @Override
    public void remove(Warehouse warehouse) {
      warehouses.remove(warehouse);
    }

    @Override
    public Warehouse findByBusinessUnitCode(String buCode) {
      return warehouses.stream()
          .filter(w -> w.archivedAt == null && buCode.equals(w.businessUnitCode))
          .findFirst()
          .orElse(null);
    }

    @Override
    public Warehouse findByIdOrBuCode(String idOrBuCode) {
      return findByBusinessUnitCode(idOrBuCode);
    }

    @Override
    public List<Warehouse> findActiveByLocation(String location) {
      return warehouses.stream()
          .filter(w -> w.archivedAt == null && location.equalsIgnoreCase(w.location))
          .toList();
    }
  }

  static class FakeLocationResolver implements LocationResolver {
    final Map<String, Location> locations = new HashMap<>();

    @Override
    public Location resolveByIdentifier(String identifier) {
      return identifier == null ? null : locations.get(identifier.toUpperCase());
    }
  }

  @BeforeEach
  public void setUp() {
    warehouseStore = new InMemoryWarehouseStore();
    locationResolver = new FakeLocationResolver();
    locationResolver.locations.put("ZWOLLE-001", new Location("ZWOLLE-001", 2, 100));
    locationResolver.locations.put("AMSTERDAM-001", new Location("AMSTERDAM-001", 1, 50));

    validator = new WarehouseValidator(warehouseStore, locationResolver);
  }

  @Test
  public void testValidateBasicAttributesValid() {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.001";
    warehouse.location = "ZWOLLE-001";
    warehouse.capacity = 50;
    warehouse.stock = 20;

    assertDoesNotThrow(() -> validator.validateBasicAttributes(warehouse));
  }

  @Test
  public void testValidateBasicAttributesNullWarehouseThrows() {
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> validator.validateBasicAttributes(null));
    assertEquals("Warehouse data cannot be null", ex.getMessage());
  }

  @Test
  public void testValidateBasicAttributesMissingBuCodeThrows() {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "  ";
    warehouse.location = "ZWOLLE-001";
    warehouse.capacity = 50;
    warehouse.stock = 10;

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> validator.validateBasicAttributes(warehouse));
    assertEquals("Business unit code is required", ex.getMessage());
  }

  @Test
  public void testValidateBasicAttributesMissingLocationThrows() {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.001";
    warehouse.location = null;
    warehouse.capacity = 50;
    warehouse.stock = 10;

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> validator.validateBasicAttributes(warehouse));
    assertEquals("Warehouse location is required", ex.getMessage());
  }

  @Test
  public void testValidateBasicAttributesInvalidCapacityThrows() {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.001";
    warehouse.location = "ZWOLLE-001";
    warehouse.capacity = 0;
    warehouse.stock = 0;

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> validator.validateBasicAttributes(warehouse));
    assertEquals("Warehouse capacity must be greater than zero", ex.getMessage());
  }

  @Test
  public void testValidateBasicAttributesNegativeStockThrows() {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.001";
    warehouse.location = "ZWOLLE-001";
    warehouse.capacity = 50;
    warehouse.stock = -5;

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> validator.validateBasicAttributes(warehouse));
    assertEquals("Warehouse stock cannot be negative", ex.getMessage());
  }

  @Test
  public void testValidateBasicAttributesStockExceedsCapacityThrows() {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.001";
    warehouse.location = "ZWOLLE-001";
    warehouse.capacity = 30;
    warehouse.stock = 35;

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> validator.validateBasicAttributes(warehouse));
    assertTrue(ex.getMessage().contains("exceeds warehouse capacity"));
  }

  @Test
  public void testValidateForCreateDuplicateBuCodeThrows() {
    Warehouse existing = new Warehouse();
    existing.businessUnitCode = "MWH.001";
    existing.location = "ZWOLLE-001";
    existing.capacity = 30;
    existing.stock = 10;
    warehouseStore.create(existing);

    Warehouse newWh = new Warehouse();
    newWh.businessUnitCode = "MWH.001";
    newWh.location = "ZWOLLE-001";
    newWh.capacity = 40;
    newWh.stock = 10;

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> validator.validateForCreate(newWh));
    assertTrue(ex.getMessage().contains("already exists"));
  }

  @Test
  public void testValidateForCreateInvalidLocationThrows() {
    Warehouse wh = new Warehouse();
    wh.businessUnitCode = "MWH.002";
    wh.location = "NONEXISTENT";
    wh.capacity = 30;
    wh.stock = 10;

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> validator.validateForCreate(wh));
    assertTrue(ex.getMessage().contains("Warehouse location is not valid"));
  }

  @Test
  public void testValidateForCreateExceedsMaxWarehousesAtLocationThrows() {
    Warehouse existing = new Warehouse();
    existing.businessUnitCode = "MWH.AMS1";
    existing.location = "AMSTERDAM-001"; // max is 1
    existing.capacity = 20;
    existing.stock = 5;
    warehouseStore.create(existing);

    Warehouse wh = new Warehouse();
    wh.businessUnitCode = "MWH.AMS2";
    wh.location = "AMSTERDAM-001";
    wh.capacity = 20;
    wh.stock = 5;

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> validator.validateForCreate(wh));
    assertTrue(ex.getMessage().contains("Maximum number of warehouses"));
  }

  @Test
  public void testValidateForCreateExceedsLocationMaxCapacityThrows() {
    Warehouse existing = new Warehouse();
    existing.businessUnitCode = "MWH.ZWL1";
    existing.location = "ZWOLLE-001"; // maxCapacity is 100, maxWh is 2
    existing.capacity = 80;
    existing.stock = 10;
    warehouseStore.create(existing);

    Warehouse wh = new Warehouse();
    wh.businessUnitCode = "MWH.ZWL2";
    wh.location = "ZWOLLE-001";
    wh.capacity = 30; // 80 + 30 = 110 > 100
    wh.stock = 10;

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> validator.validateForCreate(wh));
    assertTrue(ex.getMessage().contains("to exceed location max capacity"));
  }

  @Test
  public void testValidateForReplaceSuccess() {
    Warehouse current = new Warehouse();
    current.id = 1L;
    current.businessUnitCode = "MWH.001";
    current.location = "ZWOLLE-001";
    current.capacity = 40;
    current.stock = 15;
    warehouseStore.create(current);

    Warehouse replacement = new Warehouse();
    replacement.businessUnitCode = "MWH.001";
    replacement.location = "ZWOLLE-001";
    replacement.capacity = 60;
    replacement.stock = 15;

    assertDoesNotThrow(() -> validator.validateForReplace(replacement, current));
  }

  @Test
  public void testValidateForReplaceNullCurrentWarehouseThrows() {
    Warehouse replacement = new Warehouse();
    replacement.businessUnitCode = "MWH.001";
    replacement.location = "ZWOLLE-001";
    replacement.capacity = 60;
    replacement.stock = 15;

    assertThrows(
        WarehouseNotFoundException.class, () -> validator.validateForReplace(replacement, null));
  }

  @Test
  public void testValidateForReplaceStockMismatchThrows() {
    Warehouse current = new Warehouse();
    current.id = 1L;
    current.businessUnitCode = "MWH.001";
    current.location = "ZWOLLE-001";
    current.capacity = 40;
    current.stock = 15;

    Warehouse replacement = new Warehouse();
    replacement.businessUnitCode = "MWH.001";
    replacement.location = "ZWOLLE-001";
    replacement.capacity = 60;
    replacement.stock = 20;

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> validator.validateForReplace(replacement, current));
    assertTrue(ex.getMessage().contains("does not match previous warehouse stock"));
  }

  @Test
  public void testValidateForReplaceCapacityCannotAccommodateStockThrows() {
    Warehouse current = new Warehouse();
    current.id = 1L;
    current.businessUnitCode = "MWH.001";
    current.location = "ZWOLLE-001";
    current.capacity = 40;
    current.stock = 30;

    Warehouse replacement = new Warehouse();
    replacement.businessUnitCode = "MWH.001";
    replacement.location = "ZWOLLE-001";
    replacement.capacity = 25; // less than stock 30
    replacement.stock = 30;

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> validator.validateForReplace(replacement, current));
    assertTrue(ex.getMessage().contains("cannot accommodate"));
  }

  @Test
  public void testValidateForReplaceSameLocationExceedsMaxCapacityThrows() {
    Warehouse current = new Warehouse();
    current.id = 1L;
    current.businessUnitCode = "MWH.001";
    current.location = "ZWOLLE-001"; // max is 100
    current.capacity = 40;
    current.stock = 10;
    warehouseStore.create(current);

    Warehouse other = new Warehouse();
    other.id = 2L;
    other.businessUnitCode = "MWH.002";
    other.location = "ZWOLLE-001";
    other.capacity = 50;
    other.stock = 10;
    warehouseStore.create(other);

    Warehouse replacement = new Warehouse();
    replacement.businessUnitCode = "MWH.001";
    replacement.location = "ZWOLLE-001";
    replacement.capacity = 60; // other(50) + replacement(60) = 110 > 100
    replacement.stock = 10;

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> validator.validateForReplace(replacement, current));
    assertTrue(ex.getMessage().contains("causes total capacity to exceed location max capacity"));
  }

  @Test
  public void testValidateForReplaceDifferentLocationSuccess() {
    Warehouse current = new Warehouse();
    current.id = 1L;
    current.businessUnitCode = "MWH.001";
    current.location = "AMSTERDAM-001";
    current.capacity = 40;
    current.stock = 10;
    warehouseStore.create(current);

    Warehouse replacement = new Warehouse();
    replacement.businessUnitCode = "MWH.001";
    replacement.location = "ZWOLLE-001";
    replacement.capacity = 50;
    replacement.stock = 10;

    assertDoesNotThrow(() -> validator.validateForReplace(replacement, current));
  }

  @Test
  public void testValidateForReplaceDifferentLocationExceedsCountThrows() {
    Warehouse current = new Warehouse();
    current.id = 1L;
    current.businessUnitCode = "MWH.001";
    current.location = "ZWOLLE-001";
    current.capacity = 30;
    current.stock = 10;
    warehouseStore.create(current);

    Warehouse ams = new Warehouse();
    ams.id = 2L;
    ams.businessUnitCode = "MWH.AMS1";
    ams.location = "AMSTERDAM-001"; // max is 1
    ams.capacity = 20;
    ams.stock = 5;
    warehouseStore.create(ams);

    Warehouse replacement = new Warehouse();
    replacement.businessUnitCode = "MWH.001";
    replacement.location = "AMSTERDAM-001";
    replacement.capacity = 25;
    replacement.stock = 10;

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> validator.validateForReplace(replacement, current));
    assertTrue(ex.getMessage().contains("Maximum number of warehouses"));
  }

  @Test
  public void testValidateForReplaceDifferentLocationExceedsCapacityThrows() {
    Warehouse current = new Warehouse();
    current.id = 1L;
    current.businessUnitCode = "MWH.001";
    current.location = "AMSTERDAM-001";
    current.capacity = 40;
    current.stock = 10;
    warehouseStore.create(current);

    Warehouse replacement = new Warehouse();
    replacement.businessUnitCode = "MWH.001";
    replacement.location = "ZWOLLE-001";
    replacement.capacity = 120; // maxCapacity is 100
    replacement.stock = 10;

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> validator.validateForReplace(replacement, current));
    assertTrue(ex.getMessage().contains("exceed target location max capacity"));
  }

  @Test
  public void testValidateForReplaceInvalidLocationThrows() {
    Warehouse current = new Warehouse();
    current.id = 1L;
    current.businessUnitCode = "MWH.001";
    current.location = "ZWOLLE-001";
    current.capacity = 40;
    current.stock = 10;
    warehouseStore.create(current);

    Warehouse replacement = new Warehouse();
    replacement.businessUnitCode = "MWH.001";
    replacement.location = "INVALID-LOC";
    replacement.capacity = 40;
    replacement.stock = 10;

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> validator.validateForReplace(replacement, current));
    assertTrue(ex.getMessage().contains("Warehouse location is not valid"));
  }

  @Test
  public void testDefaultConstructor() {
    assertDoesNotThrow(() -> new WarehouseValidator());
  }
}
