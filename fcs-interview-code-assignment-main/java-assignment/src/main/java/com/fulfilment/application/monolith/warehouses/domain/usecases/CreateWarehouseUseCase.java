package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import com.fulfilment.application.monolith.warehouses.domain.validator.WarehouseValidator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDateTime;

@ApplicationScoped
public class CreateWarehouseUseCase implements CreateWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final WarehouseValidator warehouseValidator;

  @Inject
  public CreateWarehouseUseCase(WarehouseStore warehouseStore, WarehouseValidator warehouseValidator) {
    this.warehouseStore = warehouseStore;
    this.warehouseValidator = warehouseValidator;
  }

  public CreateWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this(warehouseStore, new WarehouseValidator(warehouseStore, locationResolver));
  }

  @Override
  public void create(Warehouse warehouse) {
    warehouseValidator.validateForCreate(warehouse);

    if (warehouse.createdAt == null) {
      warehouse.createdAt = LocalDateTime.now();
    }

    // if all went well, create the warehouse
    warehouseStore.create(warehouse);
  }
}
