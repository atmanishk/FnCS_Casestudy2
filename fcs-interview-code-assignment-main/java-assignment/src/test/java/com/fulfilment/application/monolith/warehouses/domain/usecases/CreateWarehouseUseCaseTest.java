package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CreateWarehouseUseCaseTest {

  private InMemoryWarehouseStore warehouseStore;
  private FakeLocationResolver locationResolver;
  private CreateWarehouseUseCase useCase;

  static class InMemoryWarehouseStore implements WarehouseStore {
    final List<Warehouse> warehouses = new ArrayList<>();

    @Override
    public List<Warehouse> getAll() {
      return warehouses.stream().filter(w -> w.archivedAt == null).toList();
    }

    @Override
    public void create(Warehouse warehouse) {
      warehouses.add(warehouse);
    }

    @Override
    public void update(Warehouse warehouse) {
      // no-op or replace in list
    }

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
    locationResolver.locations.put("ZWOLLE-001", new Location("ZWOLLE-001", 1, 40));
    locationResolver.locations.put("AMSTERDAM-001", new Location("AMSTERDAM-001", 5, 100));

    useCase = new CreateWarehouseUseCase(warehouseStore, locationResolver);
  }

  @Test
  public void testCreateWarehouseSuccessfully() {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.001";
    warehouse.location = "ZWOLLE-001";
    warehouse.capacity = 30;
    warehouse.stock = 10;

    useCase.create(warehouse);

    assertEquals(1, warehouseStore.warehouses.size());
    assertNotNull(warehouse.createdAt);
  }

  @Test
  public void testCreateWarehouseThrowsWhenNullData() {
    assertThrows(IllegalArgumentException.class, () -> useCase.create(null));
  }

  @Test
  public void testCreateWarehouseThrowsWhenBlankBusinessUnitCode() {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "   ";
    warehouse.location = "ZWOLLE-001";
    warehouse.capacity = 30;
    warehouse.stock = 10;

    assertThrows(IllegalArgumentException.class, () -> useCase.create(warehouse));
  }

  @Test
  public void testCreateWarehouseThrowsWhenDuplicateBusinessUnitCode() {
    Warehouse existing = new Warehouse();
    existing.businessUnitCode = "MWH.001";
    existing.location = "ZWOLLE-001";
    existing.capacity = 20;
    existing.stock = 5;
    warehouseStore.create(existing);

    Warehouse newWarehouse = new Warehouse();
    newWarehouse.businessUnitCode = "MWH.001";
    newWarehouse.location = "AMSTERDAM-001";
    newWarehouse.capacity = 20;
    newWarehouse.stock = 5;

    Exception ex = assertThrows(IllegalArgumentException.class, () -> useCase.create(newWarehouse));
    assertTrue(ex.getMessage().contains("already exists"));
  }

  @Test
  public void testCreateWarehouseThrowsWhenInvalidLocation() {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.002";
    warehouse.location = "NON-EXISTENT-LOC";
    warehouse.capacity = 30;
    warehouse.stock = 10;

    Exception ex = assertThrows(IllegalArgumentException.class, () -> useCase.create(warehouse));
    assertTrue(ex.getMessage().contains("location is not valid"));
  }

  @Test
  public void testCreateWarehouseThrowsWhenMaxWarehousesReachedAtLocation() {
    // ZWOLLE-001 allows max 1 warehouse
    Warehouse existing = new Warehouse();
    existing.businessUnitCode = "MWH.001";
    existing.location = "ZWOLLE-001";
    existing.capacity = 20;
    existing.stock = 5;
    warehouseStore.create(existing);

    Warehouse newWarehouse = new Warehouse();
    newWarehouse.businessUnitCode = "MWH.002";
    newWarehouse.location = "ZWOLLE-001";
    newWarehouse.capacity = 10;
    newWarehouse.stock = 2;

    Exception ex = assertThrows(IllegalArgumentException.class, () -> useCase.create(newWarehouse));
    assertTrue(ex.getMessage().contains("Maximum number of warehouses"));
  }

  @Test
  public void testCreateWarehouseThrowsWhenStockExceedsCapacity() {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.002";
    warehouse.location = "AMSTERDAM-001";
    warehouse.capacity = 20;
    warehouse.stock = 25;

    Exception ex = assertThrows(IllegalArgumentException.class, () -> useCase.create(warehouse));
    assertTrue(ex.getMessage().contains("exceeds warehouse capacity"));
  }

  @Test
  public void testCreateWarehouseThrowsWhenNegativeStockOrZeroCapacity() {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.002";
    warehouse.location = "AMSTERDAM-001";
    warehouse.capacity = 0;
    warehouse.stock = -5;

    assertThrows(IllegalArgumentException.class, () -> useCase.create(warehouse));
  }

  @Test
  public void testCreateWarehouseThrowsWhenCapacityExceedsLocationMaxCapacity() {
    // AMSTERDAM-001 max capacity is 100
    Warehouse existing = new Warehouse();
    existing.businessUnitCode = "MWH.001";
    existing.location = "AMSTERDAM-001";
    existing.capacity = 80;
    existing.stock = 10;
    warehouseStore.create(existing);

    Warehouse newWarehouse = new Warehouse();
    newWarehouse.businessUnitCode = "MWH.002";
    newWarehouse.location = "AMSTERDAM-001";
    newWarehouse.capacity = 30; // 80 + 30 = 110 > 100
    newWarehouse.stock = 10;

    Exception ex = assertThrows(IllegalArgumentException.class, () -> useCase.create(newWarehouse));
    assertTrue(ex.getMessage().contains("exceed location max capacity"));
  }
}
