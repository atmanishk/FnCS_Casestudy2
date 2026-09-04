package com.fulfilment.application.monolith.fulfillment.adapters.database;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class FulfillmentRepository implements PanacheRepository<DbFulfillment> {

  public List<DbFulfillment> findByStoreAndProduct(Long storeId, Long productId) {
    return list("storeId = ?1 and productId = ?2", storeId, productId);
  }

  public List<String> findDistinctWarehousesByStore(Long storeId) {
    return find("select distinct f.warehouseBusinessUnitCode from DbFulfillment f where f.storeId = ?1", storeId)
        .project(String.class)
        .list();
  }

  public List<Long> findDistinctProductsByWarehouse(String warehouseBuCode) {
    return find("select distinct f.productId from DbFulfillment f where f.warehouseBusinessUnitCode = ?1", warehouseBuCode)
        .project(Long.class)
        .list();
  }

  public DbFulfillment findExisting(Long storeId, Long productId, String warehouseBuCode) {
    return find("storeId = ?1 and productId = ?2 and warehouseBusinessUnitCode = ?3", storeId, productId, warehouseBuCode)
        .firstResult();
  }

  public List<DbFulfillment> findByStoreId(Long storeId) {
    return list("storeId", storeId);
  }

  public List<DbFulfillment> findByWarehouse(String warehouseBuCode) {
    return list("warehouseBusinessUnitCode", warehouseBuCode);
  }
}
