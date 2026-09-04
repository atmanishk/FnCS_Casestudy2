package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ArchiveWarehouseUseCaseTest {

  private InMemoryWarehouseStore warehouseStore;
  private ArchiveWarehouseUseCase useCase;

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
      // update
    }

    @Override
    public void remove(Warehouse warehouse) {
      warehouses.remove(warehouse);
    }

    @Override
    public Warehouse findByBusinessUnitCode(String buCode) {
      return warehouses.stream()
          .filter(w -> buCode.equals(w.businessUnitCode))
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

  @BeforeEach
  public void setUp() {
    warehouseStore = new InMemoryWarehouseStore();
    useCase = new ArchiveWarehouseUseCase(warehouseStore);
  }

  @Test
  public void testArchiveWarehouseSuccessfully() {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.001";
    warehouse.location = "ZWOLLE-001";
    warehouse.capacity = 30;
    warehouse.stock = 10;
    warehouseStore.create(warehouse);

    useCase.archive(warehouse);

    assertNotNull(warehouse.archivedAt);
    assertEquals(0, warehouseStore.getAll().size());
  }

  @Test
  public void testArchiveWarehouseThrowsWhenNull() {
    assertThrows(IllegalArgumentException.class, () -> useCase.archive(null));
  }

  @Test
  public void testArchiveWarehouseAlreadyArchivedRemainsUnchanged() {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.001";
    LocalDateTime originalArchiveTime = LocalDateTime.of(2023, 1, 1, 10, 0);
    warehouse.archivedAt = originalArchiveTime;

    useCase.archive(warehouse);

    assertEquals(originalArchiveTime, warehouse.archivedAt);
  }
}
