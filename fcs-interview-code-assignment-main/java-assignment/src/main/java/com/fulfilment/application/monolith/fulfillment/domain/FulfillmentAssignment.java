package com.fulfilment.application.monolith.fulfillment.domain;

public class FulfillmentAssignment {
  public Long id;
  public Long storeId;
  public Long productId;
  public String warehouseBusinessUnitCode;

  public FulfillmentAssignment() {}

  public FulfillmentAssignment(Long storeId, Long productId, String warehouseBusinessUnitCode) {
    this.storeId = storeId;
    this.productId = productId;
    this.warehouseBusinessUnitCode = warehouseBusinessUnitCode;
  }
}
