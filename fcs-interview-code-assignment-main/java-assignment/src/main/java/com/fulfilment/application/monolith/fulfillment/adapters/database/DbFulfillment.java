package com.fulfilment.application.monolith.fulfillment.adapters.database;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "store_product_fulfillment")
@Cacheable
public class DbFulfillment {

  @Id @GeneratedValue public Long id;

  public Long storeId;

  public Long productId;

  public String warehouseBusinessUnitCode;

  public LocalDateTime createdAt;

  public DbFulfillment() {}

  public DbFulfillment(Long storeId, Long productId, String warehouseBusinessUnitCode) {
    this.storeId = storeId;
    this.productId = productId;
    this.warehouseBusinessUnitCode = warehouseBusinessUnitCode;
    this.createdAt = LocalDateTime.now();
  }
}
