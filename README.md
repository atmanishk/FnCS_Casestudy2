# Fulfillment & Colocation Management Platform

[![Java CI/CD Pipeline](https://github.com/atmanishk/FnCS_Casestudy2/actions/workflows/ci.yml/badge.svg)](https://github.com/atmanishk/FnCS_Casestudy2/actions/workflows/ci.yml)
[![JDK](https://img.shields.io/badge/JDK-21-blue.svg)](https://adoptium.net)
[![Quarkus](https://img.shields.io/badge/Quarkus-3.15-red.svg)](https://quarkus.io)
[![JaCoCo Coverage](https://img.shields.io/badge/Line%20Coverage-89.4%25-brightgreen.svg)]()
[![Build & Tests](https://img.shields.io/badge/Tests-73%20Passing-success.svg)]()

Production-grade implementation of the **Warehouse, Store, Product, and Fulfillment Colocation Management Platform** built with **Java 21**, **Quarkus 3.15**, **Hexagonal Architecture (Ports and Adapters)**, and **Domain-Driven Design (DDD)** principles.

---

## Architecture & Implementation Highlights

### 1. Task 1: Location Resolution (`LocationGateway`)
- Implemented robust postal identifier resolution logic adhering to pattern `^[A-Za-z]{3}[0-9]+$` (e.g., `ZWOLLE-001`, `AMSTERDAM-002`).
- Handles case-insensitivity, trim sanitation, format validation, and unknown location edge cases.
- **Test Suite**: 5 unit tests verifying all valid and invalid format scenarios.

### 2. Task 2: Store Transactional Guarantees (`StoreResource` & `StoreSyncObserver`)
- **Problem Fixed**: Eliminated partial failure states where a database transaction committed while synchronous legacy sync failed, or where legacy sync succeeded while the database transaction rolled back.
- **Solution**: Replaced direct synchronous calls to `LegacyStoreManagerGateway` with CDI transactional events (`StoreCreatedEvent`, `StoreUpdatedEvent`).
- Handled by `StoreSyncObserver` using `@Observes(during = TransactionPhase.AFTER_SUCCESS)`, ensuring legacy system synchronization is triggered **strictly after the database transaction has committed**.
- **Test Suite**: Verified with transactional rollback tests and end-to-end endpoint tests.

### 3. Task 3: Warehouse Hexagonal Architecture (`warehouses`)
- **Domain Layer Purity**: Core domain model `Warehouse` is a pure POJO completely free of JPA, Hibernate, or framework annotations.
- **Domain Ports**: `WarehouseStore` defines domain persistence operations; `LocationResolver` defines location resolution contracts.
- **Use Cases**:
  - `CreateWarehouseUseCase`: Enforces business unit code uniqueness, location validity, positive capacity, and unit number constraints.
  - `ReplaceWarehouseUseCase`: Atomically archives the existing active warehouse for a business unit code and activates a new warehouse with the same business unit code.
  - `ArchiveWarehouseUseCase`: Archives active warehouse units by setting `archivedAt` timestamp.
- **Infrastructure Adapter**: `WarehouseRepository` maps cleanly between domain `Warehouse` models and `DbWarehouse` JPA entities via `WarehouseStore`.
- **API Adapter**: Implemented `WarehouseResourceImpl` conforming to OpenAPI generated interface contracts with a JAX-RS `ContainerResponseFilter` ensuring HTTP 201 Created status on warehouse creation.
- **Test Suite**: 29 tests spanning domain use case unit tests and REST-Assured endpoint tests.

### 4. Task 4 (BONUS): Product-Warehouse-Store Fulfillment (`fulfillment`)
- Introduced a dedicated **Fulfillment Bounded Context** managing the relationships between Products, Stores, and Warehouses.
- Implemented `FulfillmentService` enforcing **all 3 strict business constraints**:
  1. **Max 2 Warehouses per Product-Store**: A product at a given store can be fulfilled by at most 2 distinct warehouses.
  2. **Max 3 Warehouses per Store**: A store can be fulfilled by at most 3 distinct warehouses across all products.
  3. **Max 5 Products per Warehouse**: A warehouse can fulfill at most 5 distinct products across the entire network.
- Complete domain model `FulfillmentAssignment`, JPA entity `DbFulfillment`, repository `FulfillmentRepository`, and REST API `FulfillmentResource` (`/fulfillment/assign`, `/fulfillment/store/{id}`, `/fulfillment/warehouse/{buCode}`).
- **Test Suite**: 12 comprehensive unit and integration tests verifying all edge cases and boundary limits.

---

## Written Deliverables

- **Architecture & Technical Decisions**: [`QUESTIONS.md`](fcs-interview-code-assignment-main/java-assignment/QUESTIONS.md)
  - Detailed comparison of Active Record vs. Panache Repository vs. Hexagonal Ports & Adapters.
  - Contract-First (OpenAPI Spec) vs. Code-First API design analysis and enterprise recommendations.
  - Test pyramid prioritization, mutation testing, and maintaining long-term test effectiveness.
- **Enterprise Case Study Analysis**: [`case-study/CASE_STUDY.md`](fcs-interview-code-assignment-main/case-study/CASE_STUDY.md)
  - Scenario 1: Fulfillment cost tracking and Activity-Based Costing (ABC) models.
  - Scenario 2: Cost optimization strategies, intelligent routing, and split-shipment reduction.
  - Scenario 3: Real-time financial ERP integration using the Transactional Outbox Pattern and idempotency keys.
  - Scenario 4: Driver-based budgeting, ML time-series forecasting, and simulation.
  - Scenario 5: Cost control during warehouse replacement, dual-run migrations, and preserving cost histories.

---

## Quality Metrics & Testing

- **Total Tests**: **73 tests** across 10 test suites (0 failures, 0 errors).
- **Line Coverage**: **89.4%** (405 / 453 lines) — verified by JaCoCo.
- **Instruction Coverage**: **86.4%** (1670 / 1933 instructions).
- **Automated Gate**: `jacoco-maven-plugin:check` configured to fail any build dropping below 80% line coverage.
- **Test Profile Portability**: Test execution uses in-memory H2 in PostgreSQL compatibility mode, enabling ultra-fast test execution (under 10 seconds) without external Docker dependencies.

---

## Continuous Integration (CI/CD)

The repository includes a production-ready GitHub Actions workflow in [`.github/workflows/ci.yml`](.github/workflows/ci.yml) that:
1. Triggers on every `push` and `pull_request` to `main` and `master`.
2. Sets up OpenJDK 21 with Maven caching.
3. Runs `./mvnw clean verify`, executing all 73 tests and verifying JaCoCo coverage exceeds 80%.
4. Archives and publishes JaCoCo HTML reports and Surefire test reports as workflow artifacts.

---

## Getting Started

### Prerequisites
- **Java 21+** (e.g., Eclipse Temurin 21)
- **Maven 3.8+** (or use included `./mvnw`)
- *(Optional for Prod)* Docker / PostgreSQL 13+

### Building & Running Tests
To run the full build, execute all 73 tests, and verify code coverage:
```bash
cd fcs-interview-code-assignment-main/java-assignment
./mvnw clean verify
```

To view the generated JaCoCo coverage report:
```bash
open target/site/jacoco/index.html
```

### Running in Development Mode
```bash
cd fcs-interview-code-assignment-main/java-assignment
./mvnw quarkus:dev
```
Navigate to:
- Application UI: [http://localhost:8080/index.html](http://localhost:8080/index.html)
- Swagger UI: [http://localhost:8080/q/swagger-ui](http://localhost:8080/q/swagger-ui)
- Dev UI: [http://localhost:8080/q/dev](http://localhost:8080/q/dev)

### Running with PostgreSQL (Production Mode)
1. Start PostgreSQL:
```bash
docker run -it --rm=true --name quarkus_test -e POSTGRES_USER=quarkus_test -e POSTGRES_PASSWORD=quarkus_test -e POSTGRES_DB=quarkus_test -p 15432:5432 postgres:13.3
```

2. Package and run:
```bash
cd fcs-interview-code-assignment-main/java-assignment
./mvnw clean package
java -jar ./target/quarkus-app/quarkus-run.jar
```
