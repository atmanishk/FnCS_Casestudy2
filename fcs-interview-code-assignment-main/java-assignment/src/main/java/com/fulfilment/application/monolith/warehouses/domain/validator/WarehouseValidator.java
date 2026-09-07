package com.fulfilment.application.monolith.warehouses.domain.validator;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Objects;

@ApplicationScoped
public class WarehouseValidator {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public WarehouseValidator() {
    this.warehouseStore = null;
    this.locationResolver = null;
  }

  @Inject
  public WarehouseValidator(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  public void validateBasicAttributes(Warehouse warehouse) {
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
  }

  public void validateForCreate(Warehouse warehouse) {
    validateBasicAttributes(warehouse);

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
  }

  public void validateForReplace(Warehouse newWarehouse, Warehouse currentWarehouse) {
    if (newWarehouse == null) {
      throw new IllegalArgumentException("Replacement warehouse data cannot be null");
    }

    if (newWarehouse.businessUnitCode == null || newWarehouse.businessUnitCode.isBlank()) {
      throw new IllegalArgumentException("Business unit code is required for replacement");
    }

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
  }
}
