package com.fulfilment.application.monolith.stores;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class StoreSyncObserver {

  private static final Logger LOGGER = Logger.getLogger(StoreSyncObserver.class.getName());

  @Inject LegacyStoreManagerGateway legacyStoreManagerGateway;

  public void onStoreCreated(@Observes(during = TransactionPhase.AFTER_SUCCESS) StoreCreatedEvent event) {
    LOGGER.infof("Transaction committed: syncing newly created store '%s' to legacy system", event.store().name);
    legacyStoreManagerGateway.createStoreOnLegacySystem(event.store());
  }

  public void onStoreUpdated(@Observes(during = TransactionPhase.AFTER_SUCCESS) StoreUpdatedEvent event) {
    LOGGER.infof("Transaction committed: syncing updated store '%s' to legacy system", event.store().name);
    legacyStoreManagerGateway.updateStoreOnLegacySystem(event.store());
  }
}
