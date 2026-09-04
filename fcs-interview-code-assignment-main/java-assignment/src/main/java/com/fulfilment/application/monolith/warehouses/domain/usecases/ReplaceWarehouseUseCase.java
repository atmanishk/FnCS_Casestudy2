package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@ApplicationScoped
public class ReplaceWarehouseUseCase implements ReplaceWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public ReplaceWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  public void replace(Warehouse newWarehouse) {
    if (newWarehouse == null) {
      throw new IllegalArgumentException("Replacement warehouse data cannot be null");
    }

    if (newWarehouse.businessUnitCode == null || newWarehouse.businessUnitCode.isBlank()) {
      throw new IllegalArgumentException("Business unit code is required for replacement");
    }

    // Find the current active warehouse by businessUnitCode
    Warehouse currentWarehouse = warehouseStore.findByBusinessUnitCode(newWarehouse.businessUnitCode);
    if (currentWarehouse == null) {
      throw new WarehouseNotFoundException(
          "Active warehouse not found for business unit code: " + newWarehouse.businessUnitCode);
    }

    if (newWarehouse.location == null || newWarehouse.location.isBlank()) {
      throw new IllegalArgumentException("Warehouse location is required");
    }

    if (newWarehouse.capacity == null || newWarehouse.capacity <= 0) {
      throw new IllegalArgumentException("Warehouse capacity must be greater than zero");
    }

    if (newWarehouse.stock == null || newWarehouse.stock < 0) {
      throw new IllegalArgumentException("Warehouse stock cannot be negative");
    }

    // 1. Stock Matching: Confirm that the stock of the new warehouse matches the stock of the previous warehouse.
    if (!Objects.equals(newWarehouse.stock, currentWarehouse.stock)) {
      throw new IllegalArgumentException(
          "New warehouse stock (" + newWarehouse.stock + ") does not match previous warehouse stock ("
              + currentWarehouse.stock + ")");
    }

    // 2. Capacity Accommodation: Ensure the new warehouse's capacity can accommodate the stock from the warehouse being replaced.
    if (newWarehouse.capacity < currentWarehouse.stock) {
      throw new IllegalArgumentException(
          "New warehouse capacity (" + newWarehouse.capacity
              + ") cannot accommodate previous warehouse stock (" + currentWarehouse.stock + ")");
    }

    if (newWarehouse.stock > newWarehouse.capacity) {
      throw new IllegalArgumentException(
          "Warehouse stock (" + newWarehouse.stock + ") exceeds warehouse capacity (" + newWarehouse.capacity + ")");
    }

    // 3. Location Validation: Confirm that the warehouse location is valid
    Location location = locationResolver.resolveByIdentifier(newWarehouse.location);
    if (location == null) {
      throw new IllegalArgumentException(
          "Warehouse location is not valid: " + newWarehouse.location);
    }

    // 4. Feasibility & Capacity at the target location
    boolean isSameLocation = newWarehouse.location.equalsIgnoreCase(currentWarehouse.location);
    List<Warehouse> activeWarehousesAtLocation = warehouseStore.findActiveByLocation(newWarehouse.location);

    if (isSameLocation) {
      // Net change in capacity: current sum minus old warehouse capacity plus new warehouse capacity
      int otherWarehousesCapacitySum = activeWarehousesAtLocation.stream()
          .filter(w -> !Objects.equals(w.id, currentWarehouse.id))
          .mapToInt(w -> w.capacity != null ? w.capacity : 0)
          .sum();

      if (otherWarehousesCapacitySum + newWarehouse.capacity > location.maxCapacity) {
        throw new IllegalArgumentException(
            "New warehouse capacity (" + newWarehouse.capacity
                + ") causes total capacity to exceed location max capacity (" + location.maxCapacity + ")");
      }
    } else {
      // Moving to a new location: check both warehouse count limit and total capacity
      if (activeWarehousesAtLocation.size() >= location.maxNumberOfWarehouses) {
        throw new IllegalArgumentException(
            "Maximum number of warehouses (" + location.maxNumberOfWarehouses
                + ") already reached for target location: " + newWarehouse.location);
      }
      int currentCapacityAtNewLocation = activeWarehousesAtLocation.stream()
          .mapToInt(w -> w.capacity != null ? w.capacity : 0)
          .sum();

      if (currentCapacityAtNewLocation + newWarehouse.capacity > location.maxCapacity) {
        throw new IllegalArgumentException(
            "New warehouse capacity (" + newWarehouse.capacity
                + ") causes total capacity to exceed target location max capacity (" + location.maxCapacity + ")");
      }
    }

    // 5. Execution: Archive previous warehouse, create new warehouse with the same businessUnitCode
    currentWarehouse.archivedAt = LocalDateTime.now();
    warehouseStore.update(currentWarehouse);

    newWarehouse.businessUnitCode = currentWarehouse.businessUnitCode;
    newWarehouse.createdAt = LocalDateTime.now();
    newWarehouse.archivedAt = null;
    warehouseStore.create(newWarehouse);
  }
}
