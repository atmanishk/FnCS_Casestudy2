package com.fulfilment.application.monolith.fulfillment.domain;

import com.fulfilment.application.monolith.fulfillment.adapters.database.DbFulfillment;
import com.fulfilment.application.monolith.fulfillment.adapters.database.FulfillmentRepository;
import com.fulfilment.application.monolith.fulfillment.domain.validator.FulfillmentValidator;
import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.products.ProductRepository;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.stores.StoreRepository;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class FulfillmentService {

  @Inject StoreRepository storeRepository;
  @Inject FulfillmentRepository fulfillmentRepository;
  @Inject ProductRepository productRepository;
  @Inject WarehouseStore warehouseStore;
  @Inject FulfillmentValidator fulfillmentValidator;

  public FulfillmentService() {}

  @Inject
  public FulfillmentService(
      StoreRepository storeRepository,
      FulfillmentRepository fulfillmentRepository,
      ProductRepository productRepository,
      WarehouseStore warehouseStore,
      FulfillmentValidator fulfillmentValidator) {
    this.storeRepository = storeRepository;
    this.fulfillmentRepository = fulfillmentRepository;
    this.productRepository = productRepository;
    this.warehouseStore = warehouseStore;
    this.fulfillmentValidator = fulfillmentValidator;
  }

  public FulfillmentService(
      StoreRepository storeRepository,
      FulfillmentRepository fulfillmentRepository,
      ProductRepository productRepository,
      WarehouseStore warehouseStore) {
    this(
        storeRepository,
        fulfillmentRepository,
        productRepository,
        warehouseStore,
        new FulfillmentValidator(storeRepository, productRepository, warehouseStore, fulfillmentRepository));
  }

  @Transactional
  public DbFulfillment assignFulfillment(Long storeId, Long productId, String warehouseBuCode) {
    fulfillmentValidator.validateInput(storeId, productId, warehouseBuCode);
    fulfillmentValidator.validateEntitiesExist(storeId, productId, warehouseBuCode);

    // Check if already assigned (idempotent)
    DbFulfillment existing = fulfillmentRepository.findExisting(storeId, productId, warehouseBuCode);
    if (existing != null) {
      return existing;
    }

    // Validate the 3 business constraints
    fulfillmentValidator.validateBusinessConstraints(storeId, productId, warehouseBuCode);

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
