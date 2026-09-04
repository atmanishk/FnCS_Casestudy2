package com.fulfilment.application.monolith.stores;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class StoreSyncObserverTest {

  private TestLegacyStoreManagerGateway legacyGateway;
  private StoreSyncObserver observer;

  static class TestLegacyStoreManagerGateway extends LegacyStoreManagerGateway {
    Store createdStore;
    Store updatedStore;

    @Override
    public void createStoreOnLegacySystem(Store store) {
      this.createdStore = store;
    }

    @Override
    public void updateStoreOnLegacySystem(Store store) {
      this.updatedStore = store;
    }
  }

  @BeforeEach
  public void setUp() {
    legacyGateway = new TestLegacyStoreManagerGateway();
    observer = new StoreSyncObserver();
    observer.legacyStoreManagerGateway = legacyGateway;
  }

  @Test
  public void testOnStoreCreatedDelegatesToLegacyGateway() {
    Store store = new Store("TEST-STORE");
    store.quantityProductsInStock = 15;

    observer.onStoreCreated(new StoreCreatedEvent(store));

    assertNotNull(legacyGateway.createdStore);
    assertEquals("TEST-STORE", legacyGateway.createdStore.name);
    assertEquals(15, legacyGateway.createdStore.quantityProductsInStock);
  }

  @Test
  public void testOnStoreUpdatedDelegatesToLegacyGateway() {
    Store store = new Store("TEST-STORE-UPDATED");
    store.quantityProductsInStock = 25;

    observer.onStoreUpdated(new StoreUpdatedEvent(store));

    assertNotNull(legacyGateway.updatedStore);
    assertEquals("TEST-STORE-UPDATED", legacyGateway.updatedStore.name);
    assertEquals(25, legacyGateway.updatedStore.quantityProductsInStock);
  }
}
