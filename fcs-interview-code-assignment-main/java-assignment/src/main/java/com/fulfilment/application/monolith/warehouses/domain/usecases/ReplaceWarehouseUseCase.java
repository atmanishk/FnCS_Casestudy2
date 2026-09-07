package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import com.fulfilment.application.monolith.warehouses.domain.validator.WarehouseValidator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDateTime;

@ApplicationScoped
public class ReplaceWarehouseUseCase implements ReplaceWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final WarehouseValidator warehouseValidator;

  @Inject
  public ReplaceWarehouseUseCase(WarehouseStore warehouseStore, WarehouseValidator warehouseValidator) {
    this.warehouseStore = warehouseStore;
    this.warehouseValidator = warehouseValidator;
  }

  public ReplaceWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this(warehouseStore, new WarehouseValidator(warehouseStore, locationResolver));
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

    // Delegate validation to dedicated WarehouseValidator
    warehouseValidator.validateForReplace(newWarehouse, currentWarehouse);

    // Archive previous warehouse, create new warehouse with the same businessUnitCode
    currentWarehouse.archivedAt = LocalDateTime.now();
    warehouseStore.update(currentWarehouse);

    newWarehouse.businessUnitCode = currentWarehouse.businessUnitCode;
    newWarehouse.createdAt = LocalDateTime.now();
    newWarehouse.archivedAt = null;
    warehouseStore.create(newWarehouse);
  }
}
