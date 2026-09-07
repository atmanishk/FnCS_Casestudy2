package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;

/**
 * Maps between the OpenAPI-generated JAX-RS DTO and internal domain Warehouse model.
 * Isolates domain entities from transport layer DTOs.
 */
public final class WarehouseResourceMapper {

  private WarehouseResourceMapper() {}

  public static Warehouse toDomain(com.warehouse.api.beans.Warehouse dto) {
    if (dto == null) {
      return null;
    }
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = dto.getBusinessUnitCode();
    warehouse.location = dto.getLocation();
    warehouse.capacity = dto.getCapacity();
    warehouse.stock = dto.getStock();
    return warehouse;
  }

  public static com.warehouse.api.beans.Warehouse toResponse(Warehouse domain) {
    if (domain == null) {
      return null;
    }
    var response = new com.warehouse.api.beans.Warehouse();
    if (domain.id != null) {
      response.setId(domain.id.toString());
    }
    response.setBusinessUnitCode(domain.businessUnitCode);
    response.setLocation(domain.location);
    response.setCapacity(domain.capacity);
    response.setStock(domain.stock);
    return response;
  }
}
