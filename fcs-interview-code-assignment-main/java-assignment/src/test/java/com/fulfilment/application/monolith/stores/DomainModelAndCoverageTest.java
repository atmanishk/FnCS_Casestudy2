package com.fulfilment.application.monolith.stores;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fulfilment.application.monolith.fulfillment.adapters.restapi.FulfillmentResource;
import com.fulfilment.application.monolith.fulfillment.domain.FulfillmentAssignment;
import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class DomainModelAndCoverageTest {

  @Inject WarehouseRepository warehouseRepository;
  @Inject FulfillmentResource fulfillmentResource;

  @Test
  public void testDomainConstructorsAndModels() {
    Product p = new Product("TEST_PROD");
    assertEquals("TEST_PROD", p.name);

    Store s = new Store("TEST_STORE");
    assertEquals("TEST_STORE", s.name);

    FulfillmentAssignment fa = new FulfillmentAssignment(1L, 2L, "MWH.001");
    assertEquals(1L, fa.storeId);
    assertEquals(2L, fa.productId);
    assertEquals("MWH.001", fa.warehouseBusinessUnitCode);

    WarehouseNotFoundException wnfe = new WarehouseNotFoundException("Not found test");
    assertEquals("Not found test", wnfe.getMessage());
  }

  @Test
  public void testStoreResourceErrorMapper() {
    StoreResource.ErrorMapper mapper = new StoreResource.ErrorMapper();
    mapper.objectMapper = new ObjectMapper();

    Response resp404 = mapper.toResponse(new WebApplicationException("Custom not found", 404));
    assertEquals(404, resp404.getStatus());

    Response resp500 = mapper.toResponse(new RuntimeException("Internal failure"));
    assertEquals(500, resp500.getStatus());
  }

  @Test
  public void testWarehouseRepositoryRemove() {
    Warehouse w = new Warehouse();
    w.businessUnitCode = "TEMP.REMOVE.001";
    w.location = "ZWOLLE-002";
    w.capacity = 20;
    w.stock = 5;

    warehouseRepository.create(w);
    assertNotNull(w.id);

    warehouseRepository.remove(w);

    Warehouse w2 = new Warehouse();
    w2.businessUnitCode = "TEMP.REMOVE.002";
    w2.location = "ZWOLLE-002";
    w2.capacity = 20;
    w2.stock = 5;
    warehouseRepository.create(w2);
    w2.id = null; // test removal by businessUnitCode branch
    warehouseRepository.remove(w2);
  }

  @Test
  public void testFulfillmentResourceNullPayload() {
    assertThrows(WebApplicationException.class, () -> fulfillmentResource.assignFulfillment(null));
  }
}
