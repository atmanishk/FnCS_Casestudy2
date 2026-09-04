package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class WarehouseRepository implements WarehouseStore, PanacheRepository<DbWarehouse> {

  @Override
  public List<Warehouse> getAll() {
    return this.find("archivedAt is null").stream().map(DbWarehouse::toWarehouse).toList();
  }

  @Override
  @Transactional
  public void create(Warehouse warehouse) {
    DbWarehouse db = DbWarehouse.fromWarehouse(warehouse);
    if (db.createdAt == null) {
      db.createdAt = LocalDateTime.now();
    }
    this.persist(db);
    warehouse.id = db.id;
    warehouse.createdAt = db.createdAt;
  }

  @Override
  @Transactional
  public void update(Warehouse warehouse) {
    DbWarehouse db = null;
    if (warehouse.id != null) {
      db = this.findById(warehouse.id);
    }
    if (db == null && warehouse.businessUnitCode != null) {
      db = this.find("businessUnitCode = ?1 and archivedAt is null", warehouse.businessUnitCode)
          .firstResult();
    }
    if (db != null) {
      db.businessUnitCode = warehouse.businessUnitCode;
      db.location = warehouse.location;
      db.capacity = warehouse.capacity;
      db.stock = warehouse.stock;
      db.createdAt = warehouse.createdAt;
      db.archivedAt = warehouse.archivedAt;
      this.persist(db);
      warehouse.id = db.id;
    }
  }

  @Override
  @Transactional
  public void remove(Warehouse warehouse) {
    if (warehouse.id != null) {
      this.deleteById(warehouse.id);
    } else if (warehouse.businessUnitCode != null) {
      this.delete("businessUnitCode", warehouse.businessUnitCode);
    }
  }

  @Override
  public Warehouse findByBusinessUnitCode(String buCode) {
    if (buCode == null) {
      return null;
    }
    return this.find("businessUnitCode = ?1 and archivedAt is null", buCode.trim())
        .firstResultOptional()
        .map(DbWarehouse::toWarehouse)
        .orElse(null);
  }

  @Override
  public Warehouse findByIdOrBuCode(String idOrBuCode) {
    if (idOrBuCode == null || idOrBuCode.isBlank()) {
      return null;
    }
    try {
      Long longId = Long.parseLong(idOrBuCode.trim());
      DbWarehouse db = this.findById(longId);
      if (db != null && db.archivedAt == null) {
        return db.toWarehouse();
      }
    } catch (NumberFormatException ignored) {
      // idOrBuCode is not a numeric ID, fall through to business unit code lookup
    }
    return findByBusinessUnitCode(idOrBuCode.trim());
  }

  @Override
  public List<Warehouse> findActiveByLocation(String location) {
    if (location == null) {
      return List.of();
    }
    return this.find("location = ?1 and archivedAt is null", location.trim())
        .stream()
        .map(DbWarehouse::toWarehouse)
        .toList();
  }
}
