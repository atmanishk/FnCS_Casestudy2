package com.fulfilment.application.monolith.fulfillment.domain;

import com.fulfilment.application.monolith.fulfillment.adapters.database.DbFulfillment;
import com.fulfilment.application.monolith.fulfillment.adapters.database.FulfillmentRepository;
import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.products.ProductRepository;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.stores.StoreRepository;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class FulfillmentService {

  @Inject StoreRepository storeRepository;
  @Inject FulfillmentRepository fulfillmentRepository;
  @Inject ProductRepository productRepository;
  @Inject WarehouseStore warehouseStore;

  public FulfillmentService() {}

  public FulfillmentService(
      StoreRepository storeRepository,
      FulfillmentRepository fulfillmentRepository,
      ProductRepository productRepository,
      WarehouseStore warehouseStore) {
    this.storeRepository = storeRepository;
    this.fulfillmentRepository = fulfillmentRepository;
    this.productRepository = productRepository;
    this.warehouseStore = warehouseStore;
  }

  @Transactional
  public DbFulfillment assignFulfillment(Long storeId, Long productId, String warehouseBuCode) {
    if (storeId == null || productId == null || warehouseBuCode == null || warehouseBuCode.isBlank()) {
      throw new IllegalArgumentException("storeId, productId, and warehouseBusinessUnitCode are required");
    }

    // Verify Store exists
    Store store = storeRepository.findById(storeId);
    if (store == null) {
      throw new WebApplicationException("Store with id " + storeId + " not found", 404);
    }

    // Verify Product exists
    Product product = productRepository.findById(productId);
    if (product == null) {
      throw new WebApplicationException("Product with id " + productId + " not found", 404);
    }

    // Verify Warehouse exists and is active
    Warehouse warehouse = warehouseStore.findByBusinessUnitCode(warehouseBuCode);
    if (warehouse == null) {
      throw new WebApplicationException(
          "Active warehouse with businessUnitCode " + warehouseBuCode + " not found", 404);
    }

    // Check if already assigned (idempotent)
    DbFulfillment existing = fulfillmentRepository.findExisting(storeId, productId, warehouseBuCode);
    if (existing != null) {
      return existing;
    }

    // Constraint 1: Each Product can be fulfilled by a maximum of 2 different Warehouses per Store
    List<DbFulfillment> currentForProductAndStore = fulfillmentRepository.findByStoreAndProduct(storeId, productId);
    Set<String> warehousesForProductStore = currentForProductAndStore.stream()
        .map(f -> f.warehouseBusinessUnitCode)
        .collect(Collectors.toSet());
    if (!warehousesForProductStore.contains(warehouseBuCode) && warehousesForProductStore.size() >= 2) {
      throw new IllegalArgumentException(
          "Constraint 1 violated: Product " + productId + " is already fulfilled by 2 warehouses for store " + storeId);
    }

    // Constraint 2: Each Store can be fulfilled by a maximum of 3 different Warehouses
    List<String> distinctWarehousesForStore = fulfillmentRepository.findDistinctWarehousesByStore(storeId);
    if (!distinctWarehousesForStore.contains(warehouseBuCode) && distinctWarehousesForStore.size() >= 3) {
      throw new IllegalArgumentException(
          "Constraint 2 violated: Store " + storeId + " is already fulfilled by 3 different warehouses");
    }

    // Constraint 3: Each Warehouse can store maximally 5 types of Products
    List<Long> distinctProductsForWarehouse = fulfillmentRepository.findDistinctProductsByWarehouse(warehouseBuCode);
    if (!distinctProductsForWarehouse.contains(productId) && distinctProductsForWarehouse.size() >= 5) {
      throw new IllegalArgumentException(
          "Constraint 3 violated: Warehouse " + warehouseBuCode + " is already fulfilling 5 different products");
    }

    DbFulfillment newFulfillment = new DbFulfillment(storeId, productId, warehouseBuCode);
    fulfillmentRepository.persist(newFulfillment);
    return newFulfillment;
  }

  public List<DbFulfillment> getFulfillmentsByStore(Long storeId) {
    return fulfillmentRepository.findByStoreId(storeId);
  }

  public List<DbFulfillment> getFulfillmentsByWarehouse(String warehouseBuCode) {
    return fulfillmentRepository.findByWarehouse(warehouseBuCode);
  }

  @Transactional
  public boolean removeFulfillment(Long id) {
    return fulfillmentRepository.deleteById(id);
  }
}
