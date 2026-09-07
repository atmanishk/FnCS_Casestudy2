package com.fulfilment.application.monolith.fulfillment.domain;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fulfilment.application.monolith.fulfillment.adapters.database.DbFulfillment;
import com.fulfilment.application.monolith.fulfillment.adapters.database.FulfillmentRepository;
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

public class FulfillmentValidatorTest {

  private TestStoreRepository storeRepo;
  private TestFulfillmentRepository fulfillmentRepo;
  private TestProductRepository productRepo;
  private TestWarehouseStore warehouseStore;
  private FulfillmentValidator validator;

  static class TestStoreRepository extends StoreRepository {
    final List<Store> stores = new ArrayList<>();

    @Override
    public Store findById(Long id) {
      return stores.stream().filter(s -> id.equals(s.id)).findFirst().orElse(null);
    }
  }

  static class TestFulfillmentRepository extends FulfillmentRepository {
    final List<DbFulfillment> list = new ArrayList<>();

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
    public List<Long> findDistinctProductsByWarehouse(String buCode) {
      return list.stream()
          .filter(f -> buCode.equals(f.warehouseBusinessUnitCode))
          .map(f -> f.productId)
          .distinct()
          .toList();
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

    validator = new FulfillmentValidator(storeRepo, productRepo, warehouseStore, fulfillmentRepo);

    Store store = new Store();
    store.id = 1L;
    store.name = "Test Store";
    storeRepo.stores.add(store);

    Product product = new Product();
    product.id = 10L;
    product.name = "Test Product";
    productRepo.products.add(product);

    Warehouse wh = new Warehouse();
    wh.id = 100L;
    wh.businessUnitCode = "MWH.001";
    wh.location = "ZWOLLE-001";
    wh.capacity = 50;
    wh.stock = 10;
    warehouseStore.create(wh);
  }

  @Test
  public void testValidateInputSuccess() {
    assertDoesNotThrow(() -> validator.validateInput(1L, 10L, "MWH.001"));
  }

  @Test
  public void testValidateInputNullStoreIdThrows() {
    assertThrows(IllegalArgumentException.class, () -> validator.validateInput(null, 10L, "MWH.001"));
  }

  @Test
  public void testValidateInputNullProductIdThrows() {
    assertThrows(IllegalArgumentException.class, () -> validator.validateInput(1L, null, "MWH.001"));
  }

  @Test
  public void testValidateInputBlankBuCodeThrows() {
    assertThrows(IllegalArgumentException.class, () -> validator.validateInput(1L, 10L, "   "));
  }

  @Test
  public void testValidateEntitiesExistSuccess() {
    assertDoesNotThrow(() -> validator.validateEntitiesExist(1L, 10L, "MWH.001"));
  }

  @Test
  public void testValidateEntitiesExistStoreNotFoundThrows404() {
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> validator.validateEntitiesExist(999L, 10L, "MWH.001"));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void testValidateEntitiesExistProductNotFoundThrows404() {
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> validator.validateEntitiesExist(1L, 999L, "MWH.001"));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void testValidateEntitiesExistWarehouseNotFoundThrows404() {
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> validator.validateEntitiesExist(1L, 10L, "MWH.NONEXISTENT"));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void testConstraint1Max2WarehousesPerProductStore() {
    fulfillmentRepo.list.add(new DbFulfillment(1L, 10L, "MWH.001"));
    fulfillmentRepo.list.add(new DbFulfillment(1L, 10L, "MWH.002"));

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> validator.validateBusinessConstraints(1L, 10L, "MWH.003"));
    assertTrue(ex.getMessage().contains("Constraint 1 violated"));
  }

  @Test
  public void testConstraint2Max3WarehousesPerStore() {
    fulfillmentRepo.list.add(new DbFulfillment(1L, 10L, "MWH.001"));
    fulfillmentRepo.list.add(new DbFulfillment(1L, 11L, "MWH.002"));
    fulfillmentRepo.list.add(new DbFulfillment(1L, 12L, "MWH.003"));

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> validator.validateBusinessConstraints(1L, 13L, "MWH.004"));
    assertTrue(ex.getMessage().contains("Constraint 2 violated"));
  }

  @Test
  public void testConstraint3Max5ProductsPerWarehouse() {
    fulfillmentRepo.list.add(new DbFulfillment(1L, 1L, "MWH.001"));
    fulfillmentRepo.list.add(new DbFulfillment(1L, 2L, "MWH.001"));
    fulfillmentRepo.list.add(new DbFulfillment(1L, 3L, "MWH.001"));
    fulfillmentRepo.list.add(new DbFulfillment(2L, 4L, "MWH.001"));
    fulfillmentRepo.list.add(new DbFulfillment(2L, 5L, "MWH.001"));

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> validator.validateBusinessConstraints(3L, 6L, "MWH.001"));
    assertTrue(ex.getMessage().contains("Constraint 3 violated"));
  }

  @Test
  public void testValidateInputNullBuCodeThrows() {
    assertThrows(IllegalArgumentException.class, () -> validator.validateInput(1L, 10L, null));
  }

  @Test
  public void testConstraint1AllowsExistingWarehouseReassignment() {
    // If MWH.001 is already fulfilling, it shouldn't trigger violation even if count is 2
    fulfillmentRepo.list.add(new DbFulfillment(1L, 10L, "MWH.001"));
    fulfillmentRepo.list.add(new DbFulfillment(1L, 10L, "MWH.002"));

    assertDoesNotThrow(() -> validator.validateBusinessConstraints(1L, 10L, "MWH.001"));
  }

  @Test
  public void testConstraint2AllowsExistingWarehouseReassignment() {
    fulfillmentRepo.list.add(new DbFulfillment(1L, 10L, "MWH.001"));
    fulfillmentRepo.list.add(new DbFulfillment(1L, 11L, "MWH.002"));
    fulfillmentRepo.list.add(new DbFulfillment(1L, 12L, "MWH.003"));

    assertDoesNotThrow(() -> validator.validateBusinessConstraints(1L, 13L, "MWH.001"));
  }

  @Test
  public void testConstraint3AllowsExistingProductReassignment() {
    fulfillmentRepo.list.add(new DbFulfillment(1L, 1L, "MWH.001"));
    fulfillmentRepo.list.add(new DbFulfillment(1L, 2L, "MWH.001"));
    fulfillmentRepo.list.add(new DbFulfillment(1L, 3L, "MWH.001"));
    fulfillmentRepo.list.add(new DbFulfillment(2L, 4L, "MWH.001"));
    fulfillmentRepo.list.add(new DbFulfillment(2L, 5L, "MWH.001"));

    assertDoesNotThrow(() -> validator.validateBusinessConstraints(3L, 1L, "MWH.001"));
  }

  @Test
  public void testValidateAssignmentFullFlow() {
    assertDoesNotThrow(() -> validator.validateAssignment(1L, 10L, "MWH.001"));
  }

  @Test
  public void testDefaultConstructor() {
    assertDoesNotThrow(() -> new FulfillmentValidator());
  }
}
