package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

public class ReplaceWarehouseUseCaseTest {

  private InMemoryWarehouseStore warehouseStore;
  private FakeLocationResolver locationResolver;
  private ReplaceWarehouseUseCase useCase;

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
    public void update(Warehouse warehouse) {
      // already in list, fields updated
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
    locationResolver.locations.put("TILBURG-001", new Location("TILBURG-001", 1, 40));

    useCase = new ReplaceWarehouseUseCase(warehouseStore, locationResolver);
  }

  @Test
  public void testReplaceWarehouseSuccessfullyInSameLocation() {
    Warehouse oldWarehouse = new Warehouse();
    oldWarehouse.businessUnitCode = "MWH.001";
    oldWarehouse.location = "ZWOLLE-001";
    oldWarehouse.capacity = 30;
    oldWarehouse.stock = 10;
    warehouseStore.create(oldWarehouse);

    Warehouse newWarehouse = new Warehouse();
    newWarehouse.businessUnitCode = "MWH.001";
    newWarehouse.location = "ZWOLLE-001";
    newWarehouse.capacity = 35;
    newWarehouse.stock = 10; // matches old stock

    useCase.replace(newWarehouse);

    assertNotNull(oldWarehouse.archivedAt);
    assertNotNull(newWarehouse.createdAt);
    assertEquals("MWH.001", newWarehouse.businessUnitCode);
    assertEquals(1, warehouseStore.getAll().size());
  }

  @Test
  public void testReplaceWarehouseThrowsWhenWarehouseNotFound() {
    Warehouse newWarehouse = new Warehouse();
    newWarehouse.businessUnitCode = "NON-EXISTENT";
    newWarehouse.location = "ZWOLLE-001";
    newWarehouse.capacity = 35;
    newWarehouse.stock = 10;

    assertThrows(WarehouseNotFoundException.class, () -> useCase.replace(newWarehouse));
  }

  @Test
  public void testReplaceWarehouseThrowsWhenStockDoesNotMatch() {
    Warehouse oldWarehouse = new Warehouse();
    oldWarehouse.businessUnitCode = "MWH.001";
    oldWarehouse.location = "ZWOLLE-001";
    oldWarehouse.capacity = 30;
    oldWarehouse.stock = 10;
    warehouseStore.create(oldWarehouse);

    Warehouse newWarehouse = new Warehouse();
    newWarehouse.businessUnitCode = "MWH.001";
    newWarehouse.location = "ZWOLLE-001";
    newWarehouse.capacity = 35;
    newWarehouse.stock = 12; // doesn't match 10

    Exception ex = assertThrows(IllegalArgumentException.class, () -> useCase.replace(newWarehouse));
    assertTrue(ex.getMessage().contains("does not match previous warehouse stock"));
  }

  @Test
  public void testReplaceWarehouseThrowsWhenNewCapacityCannotAccommodateStock() {
    Warehouse oldWarehouse = new Warehouse();
    oldWarehouse.businessUnitCode = "MWH.001";
    oldWarehouse.location = "ZWOLLE-001";
    oldWarehouse.capacity = 30;
    oldWarehouse.stock = 25;
    warehouseStore.create(oldWarehouse);

    Warehouse newWarehouse = new Warehouse();
    newWarehouse.businessUnitCode = "MWH.001";
    newWarehouse.location = "ZWOLLE-001";
    newWarehouse.capacity = 20; // 20 < 25
    newWarehouse.stock = 25;

    Exception ex = assertThrows(IllegalArgumentException.class, () -> useCase.replace(newWarehouse));
    assertTrue(ex.getMessage().contains("cannot accommodate previous warehouse stock"));
  }

  @Test
  public void testReplaceWarehouseThrowsWhenExceedingLocationMaxCapacity() {
    Warehouse oldWarehouse = new Warehouse();
    oldWarehouse.businessUnitCode = "MWH.001";
    oldWarehouse.location = "ZWOLLE-001";
    oldWarehouse.capacity = 30;
    oldWarehouse.stock = 10;
    warehouseStore.create(oldWarehouse);

    Warehouse newWarehouse = new Warehouse();
    newWarehouse.businessUnitCode = "MWH.001";
    newWarehouse.location = "ZWOLLE-001";
    newWarehouse.capacity = 50; // maxCapacity is 40
    newWarehouse.stock = 10;

    Exception ex = assertThrows(IllegalArgumentException.class, () -> useCase.replace(newWarehouse));
    assertTrue(ex.getMessage().contains("exceed location max capacity"));
  }
}
