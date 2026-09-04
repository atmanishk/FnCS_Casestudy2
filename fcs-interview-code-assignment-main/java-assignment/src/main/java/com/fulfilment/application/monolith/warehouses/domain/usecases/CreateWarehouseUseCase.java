package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class CreateWarehouseUseCase implements CreateWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public CreateWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  public void create(Warehouse warehouse) {
    if (warehouse == null) {
      throw new IllegalArgumentException("Warehouse data cannot be null");
    }

    if (warehouse.businessUnitCode == null || warehouse.businessUnitCode.isBlank()) {
      throw new IllegalArgumentException("Business unit code is required");
    }

    if (warehouse.location == null || warehouse.location.isBlank()) {
      throw new IllegalArgumentException("Warehouse location is required");
    }

    if (warehouse.capacity == null || warehouse.capacity <= 0) {
      throw new IllegalArgumentException("Warehouse capacity must be greater than zero");
    }

    if (warehouse.stock == null || warehouse.stock < 0) {
      throw new IllegalArgumentException("Warehouse stock cannot be negative");
    }

    if (warehouse.stock > warehouse.capacity) {
      throw new IllegalArgumentException(
          "Warehouse stock (" + warehouse.stock + ") exceeds warehouse capacity (" + warehouse.capacity + ")");
    }

    // 1. Business Unit Code Verification: BU code doesn't already exist
    Warehouse existingBuCode = warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode);
    if (existingBuCode != null) {
      throw new IllegalArgumentException(
          "Warehouse with business unit code already exists: " + warehouse.businessUnitCode);
    }

    // 2. Location Validation: Must be an existing valid location
    Location location = locationResolver.resolveByIdentifier(warehouse.location);
    if (location == null) {
      throw new IllegalArgumentException(
          "Warehouse location is not valid: " + warehouse.location);
    }

    // 3. Warehouse Creation Feasibility: Check max number of warehouses at location
    List<Warehouse> activeWarehousesAtLocation = warehouseStore.findActiveByLocation(warehouse.location);
    if (activeWarehousesAtLocation.size() >= location.maxNumberOfWarehouses) {
      throw new IllegalArgumentException(
          "Maximum number of warehouses (" + location.maxNumberOfWarehouses + ") already reached for location: " + warehouse.location);
    }

    // 4. Capacity and Stock Validation: Total capacity at location
    int currentCapacityAtLocation = activeWarehousesAtLocation.stream()
        .mapToInt(w -> w.capacity != null ? w.capacity : 0)
        .sum();

    if (currentCapacityAtLocation + warehouse.capacity > location.maxCapacity) {
      throw new IllegalArgumentException(
          "Warehouse capacity (" + warehouse.capacity + ") causes total capacity ("
              + (currentCapacityAtLocation + warehouse.capacity)
              + ") to exceed location max capacity (" + location.maxCapacity + ")");
    }

    if (warehouse.createdAt == null) {
      warehouse.createdAt = LocalDateTime.now();
    }

    // if all went well, create the warehouse
    warehouseStore.create(warehouse);
  }
}
