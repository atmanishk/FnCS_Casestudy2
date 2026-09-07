package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import org.junit.jupiter.api.Test;

public class WarehouseResourceMapperTest {

  @Test
  public void testToDomainWithValidDto() {
    var dto = new com.warehouse.api.beans.Warehouse();
    dto.setBusinessUnitCode("MWH.100");
    dto.setLocation("ZWOLLE-001");
    dto.setCapacity(100);
    dto.setStock(20);

    Warehouse domain = WarehouseResourceMapper.toDomain(dto);
    assertNotNull(domain);
    assertEquals("MWH.100", domain.businessUnitCode);
    assertEquals("ZWOLLE-001", domain.location);
    assertEquals(100, domain.capacity);
    assertEquals(20, domain.stock);
  }

  @Test
  public void testToDomainNullReturnsNull() {
    assertNull(WarehouseResourceMapper.toDomain(null));
  }

  @Test
  public void testToResponseWithValidDomain() {
    Warehouse domain = new Warehouse();
    domain.id = 5L;
    domain.businessUnitCode = "MWH.100";
    domain.location = "ZWOLLE-001";
    domain.capacity = 100;
    domain.stock = 20;

    var dto = WarehouseResourceMapper.toResponse(domain);
    assertNotNull(dto);
    assertEquals("5", dto.getId());
    assertEquals("MWH.100", dto.getBusinessUnitCode());
    assertEquals("ZWOLLE-001", dto.getLocation());
    assertEquals(100, dto.getCapacity());
    assertEquals(20, dto.getStock());
  }

  @Test
  public void testToResponseWithNullId() {
    Warehouse domain = new Warehouse();
    domain.id = null;
    domain.businessUnitCode = "MWH.100";
    domain.location = "ZWOLLE-001";
    domain.capacity = 100;
    domain.stock = 20;

    var dto = WarehouseResourceMapper.toResponse(domain);
    assertNotNull(dto);
    assertNull(dto.getId());
  }

  @Test
  public void testToResponseNullReturnsNull() {
    assertNull(WarehouseResourceMapper.toResponse(null));
  }
}
