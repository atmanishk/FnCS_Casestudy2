package com.fulfilment.application.monolith.fulfillment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fulfilment.application.monolith.fulfillment.adapters.database.DbFulfillment;
import com.fulfilment.application.monolith.fulfillment.adapters.database.FulfillmentRepository;
import com.fulfilment.application.monolith.fulfillment.domain.FulfillmentService;
import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.products.ProductRepository;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.stores.StoreRepository;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FulfillmentServiceTest {

  private TestStoreRepository storeRepo;
  private TestFulfillmentRepository fulfillmentRepo;
  private TestProductRepository productRepo;
  private TestWarehouseStore warehouseStore;
  private FulfillmentService service;

  static class TestStoreRepository extends StoreRepository {
    final List<Store> stores = new ArrayList<>();

    @Override
    public Store findById(Long id) {
      return stores.stream().filter(s -> id.equals(s.id)).findFirst().orElse(null);
    }
  }

  static class TestFulfillmentRepository extends FulfillmentRepository {
    final List<DbFulfillment> list = new ArrayList<>();
    long seq = 1;

    @Override
    public List<DbFulfillment> findByStoreAndProduct(Long storeId, Long productId) {
      return list.stream()
          .filter(f -> storeId.equals(f.storeId) && productId.equals(f.productId))
          .toList();
    }

    @Override
    public List<String> findDistinctWarehousesByStore(Long storeId) {
      return list.stream()
          .filter(f -> storeId.equals(f.storeId))
          .map(f -> f.warehouseBusinessUnitCode)
          .distinct()
          .toList();
    }

    @Override
    public List<Long> findDistinctProductsByWarehouse(String warehouseBuCode) {
      return list.stream()
          .filter(f -> warehouseBuCode.equals(f.warehouseBusinessUnitCode))
          .map(f -> f.productId)
          .distinct()
          .toList();
    }

    @Override
    public DbFulfillment findExisting(Long storeId, Long productId, String warehouseBuCode) {
      return list.stream()
          .filter(f -> storeId.equals(f.storeId) && productId.equals(f.productId) && warehouseBuCode.equals(f.warehouseBusinessUnitCode))
          .findFirst()
          .orElse(null);
    }

    @Override
    public void persist(DbFulfillment f) {
      if (f.id == null) {
        f.id = seq++;
      }
      list.add(f);
    }

    @Override
    public List<DbFulfillment> findByStoreId(Long storeId) {
      return list.stream().filter(f -> storeId.equals(f.storeId)).toList();
    }

    @Override
    public List<DbFulfillment> findByWarehouse(String warehouseBuCode) {
      return list.stream().filter(f -> warehouseBuCode.equals(f.warehouseBusinessUnitCode)).toList();
    }

    @Override
    public boolean deleteById(Long id) {
      return list.removeIf(f -> id.equals(f.id));
    }
  }

  static class TestProductRepository extends ProductRepository {
    final List<Product> products = new ArrayList<>();

    @Override
    public Product findById(Long id) {
      return products.stream().filter(p -> id.equals(p.id)).findFirst().orElse(null);
    }
  }

  static class TestWarehouseStore implements WarehouseStore {
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
    public void update(Warehouse warehouse) {}

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

  @BeforeEach
  public void setUp() {
    storeRepo = new TestStoreRepository();
    fulfillmentRepo = new TestFulfillmentRepository();
    productRepo = new TestProductRepository();
    warehouseStore = new TestWarehouseStore();
    service = new FulfillmentService(storeRepo, fulfillmentRepo, productRepo, warehouseStore);

    Store s1 = new Store("TONSTAD"); s1.id = 1L; storeRepo.stores.add(s1);
    Store s2 = new Store("KALLAX"); s2.id = 2L; storeRepo.stores.add(s2);

    Product p1 = new Product("TONSTAD"); p1.id = 1L; productRepo.products.add(p1);
    Product p2 = new Product("KALLAX"); p2.id = 2L; productRepo.products.add(p2);
    Product p3 = new Product("BESTÅ"); p3.id = 3L; productRepo.products.add(p3);
    Product p4 = new Product("BILLY"); p4.id = 4L; productRepo.products.add(p4);
    Product p5 = new Product("POÄNG"); p5.id = 5L; productRepo.products.add(p5);
    Product p6 = new Product("MALM"); p6.id = 6L; productRepo.products.add(p6);

    Warehouse w1 = new Warehouse(); w1.businessUnitCode = "MWH.001"; w1.capacity = 100; warehouseStore.create(w1);
    Warehouse w2 = new Warehouse(); w2.businessUnitCode = "MWH.002"; w2.capacity = 100; warehouseStore.create(w2);
    Warehouse w3 = new Warehouse(); w3.businessUnitCode = "MWH.003"; w3.capacity = 100; warehouseStore.create(w3);
    Warehouse w4 = new Warehouse(); w4.businessUnitCode = "MWH.004"; w4.capacity = 100; warehouseStore.create(w4);
  }

  @Test
  public void testAssignFulfillmentSuccessfully() {
    DbFulfillment f = service.assignFulfillment(1L, 1L, "MWH.001");

    assertNotNull(f);
    assertNotNull(f.id);
    assertEquals(1L, f.storeId);
    assertEquals(1L, f.productId);
    assertEquals("MWH.001", f.warehouseBusinessUnitCode);
  }

  @Test
  public void testAssignFulfillmentIsIdempotent() {
    DbFulfillment f1 = service.assignFulfillment(1L, 1L, "MWH.001");
    DbFulfillment f2 = service.assignFulfillment(1L, 1L, "MWH.001");

    assertEquals(f1.id, f2.id);
    assertEquals(1, fulfillmentRepo.list.size());
  }

  @Test
  public void testConstraint1_Max2WarehousesPerProductPerStore() {
    service.assignFulfillment(1L, 1L, "MWH.001");
    service.assignFulfillment(1L, 1L, "MWH.002");

    // 3rd warehouse for same product and store should fail
    Exception ex = assertThrows(IllegalArgumentException.class, () ->
        service.assignFulfillment(1L, 1L, "MWH.003")
    );
    assertTrue(ex.getMessage().contains("Constraint 1 violated"));
  }

  @Test
  public void testConstraint2_Max3WarehousesPerStore() {
    service.assignFulfillment(1L, 1L, "MWH.001");
    service.assignFulfillment(1L, 2L, "MWH.002");
    service.assignFulfillment(1L, 3L, "MWH.003");

    // 4th distinct warehouse for this store should fail
    Exception ex = assertThrows(IllegalArgumentException.class, () ->
        service.assignFulfillment(1L, 4L, "MWH.004")
    );
    assertTrue(ex.getMessage().contains("Constraint 2 violated"));
  }

  @Test
  public void testConstraint3_Max5ProductsPerWarehouse() {
    service.assignFulfillment(1L, 1L, "MWH.001");
    service.assignFulfillment(1L, 2L, "MWH.001");
    service.assignFulfillment(2L, 3L, "MWH.001");
    service.assignFulfillment(2L, 4L, "MWH.001");
    service.assignFulfillment(2L, 5L, "MWH.001");

    // 6th distinct product for MWH.001 should fail
    Exception ex = assertThrows(IllegalArgumentException.class, () ->
        service.assignFulfillment(1L, 6L, "MWH.001")
    );
    assertTrue(ex.getMessage().contains("Constraint 3 violated"));
  }

  @Test
  public void testAssignThrowsWhenStoreNotFound() {
    assertThrows(WebApplicationException.class, () ->
        service.assignFulfillment(999L, 1L, "MWH.001")
    );
  }

  @Test
  public void testAssignThrowsWhenProductNotFound() {
    assertThrows(WebApplicationException.class, () ->
        service.assignFulfillment(1L, 999L, "MWH.001")
    );
  }

  @Test
  public void testAssignThrowsWhenWarehouseNotFound() {
    assertThrows(WebApplicationException.class, () ->
        service.assignFulfillment(1L, 1L, "NON-EXISTENT-WH")
    );
  }

  @Test
  public void testGetFulfillmentsByStoreAndWarehouseAndRemove() {
    DbFulfillment f1 = service.assignFulfillment(1L, 1L, "MWH.001");
    DbFulfillment f2 = service.assignFulfillment(1L, 2L, "MWH.001");

    assertEquals(2, service.getFulfillmentsByStore(1L).size());
    assertEquals(2, service.getFulfillmentsByWarehouse("MWH.001").size());

    assertTrue(service.removeFulfillment(f1.id));
    assertEquals(1, service.getFulfillmentsByStore(1L).size());
  }
}
