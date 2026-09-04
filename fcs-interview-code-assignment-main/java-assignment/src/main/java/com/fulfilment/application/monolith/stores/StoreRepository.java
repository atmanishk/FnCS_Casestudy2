package com.fulfilment.application.monolith.stores;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class StoreRepository implements PanacheRepository<Store> {
  // Provides repository abstraction for Store entity
}
