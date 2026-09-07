# Fulfillment & Colocation Management Platform

[![Java CI/CD Pipeline](https://github.com/atmanishk/FnCS_Casestudy2/actions/workflows/ci.yml/badge.svg)](https://github.com/atmanishk/FnCS_Casestudy2/actions/workflows/ci.yml)
[![JDK](https://img.shields.io/badge/JDK-21-blue.svg)](https://adoptium.net)
[![Quarkus](https://img.shields.io/badge/Quarkus-3.15-red.svg)](https://quarkus.io)
[![JaCoCo Coverage](https://img.shields.io/badge/Line%20Coverage-86.85%25-brightgreen.svg)](docs/TEST_COVERAGE_REPORT.md)
[![Build & Tests](https://img.shields.io/badge/Tests-111%20Passing-success.svg)](docs/TEST_COVERAGE_REPORT.md)

Production-grade implementation of the **Warehouse, Store, Product, and Fulfillment Colocation Management Platform** built with **Java 21**, **Quarkus 3.15**, **Hexagonal Architecture (Ports and Adapters)**, and **Domain-Driven Design (DDD)** principles.

---

## Candidate Submission Update & Feedback Resolution

In response to technical review feedback, the following architectural enhancements and deliverables were completed:

| # | Reviewer Feedback | Architectural Resolution | Evidence / Location |
|---|---|---|---|
| **1** | **Separate out validation logic for Warehouse and Fulfillment** | • Extracted `WarehouseValidator` domain service from `CreateWarehouseUseCase` and `ReplaceWarehouseUseCase`.<br>• Extracted `FulfillmentValidator` component from `FulfillmentService` isolating input checks, entity existence checks, and the 3 business constraint rules.<br>• Use cases and services now strictly adhere to Single Responsibility Principle (SRP). | • [`WarehouseValidator.java`](fcs-interview-code-assignment-main/java-assignment/src/main/java/com/fulfilment/application/monolith/warehouses/domain/validator/WarehouseValidator.java)<br>• [`FulfillmentValidator.java`](fcs-interview-code-assignment-main/java-assignment/src/main/java/com/fulfilment/application/monolith/fulfillment/domain/FulfillmentValidator.java) |
| **2** | **Add screenshots of the running application** | Captured high-resolution screenshots of the live system including Interactive Swagger UI, Product Management Web UI, Quarkus Dev UI, and JaCoCo Coverage Report. | See [Application Visual Showcase](#application-visual-showcase) and [`docs/screenshots/`](docs/screenshots/) |
| **3** | **Source code coverage > 80% & include coverage report for tracking** | • **86.85% Line Coverage** (436/502 lines) & **82.78% Instruction Coverage** (1,735/2,096 instructions).<br>• **111 automated tests** passing across 12 suites.<br>• Detailed coverage breakdown published with package/class metrics. | • [`docs/TEST_COVERAGE_REPORT.md`](docs/TEST_COVERAGE_REPORT.md)<br>• [`docs/jacoco-report/index.html`](docs/jacoco-report/index.html) |
| **4** | **Remove unused / duplicate files** | Removed redundant duplicate files at root (`CASE_STUDY.md`, `Question.md`) and in `java-assignment/Question.md`. Preserved canonical versions in their respective module locations. | • [`case-study/CASE_STUDY.md`](fcs-interview-code-assignment-main/case-study/CASE_STUDY.md)<br>• [`java-assignment/QUESTIONS.md`](fcs-interview-code-assignment-main/java-assignment/QUESTIONS.md) |

---

## Application Visual Showcase

### 1. Interactive Swagger UI (`/q/swagger-ui`)
Fully interactive OpenAPI 3.0 specification documenting all Warehouse, Store, Product, and Fulfillment endpoints:
![Swagger UI](docs/screenshots/swagger-ui.png)

### 2. Live Application Web UI (`/index.html`)
Live responsive management interface powered by Panache ORM and PostgreSQL:
![Web Application UI](docs/screenshots/web-app-ui.png)

### 3. Quarkus Dev UI (`/q/dev-ui`)
Live runtime dashboard showing CDI beans, Hibernate ORM entity mappings, REST endpoints, and extensions:
![Quarkus Dev UI](docs/screenshots/quarkus-dev-ui.png)

### 4. JaCoCo Test Coverage Report
Automated verification showing **86.85% line coverage** across all packages:
![JaCoCo Code Coverage Report](docs/screenshots/jacoco-coverage-report.png)

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
- **Dedicated Domain Validator**: `WarehouseValidator` encapsulates all business constraints:
  - BU code uniqueness
  - Postal location validity
  - Max active warehouse count per location
  - Cumulative capacity limits per location
  - Atomic replacement stock matching and capacity accommodation
- **Use Cases**:
  - `CreateWarehouseUseCase`: Orchestrates warehouse creation delegating validation to `WarehouseValidator`.
  - `ReplaceWarehouseUseCase`: Atomically archives the existing active warehouse and activates the replacement unit.
  - `ArchiveWarehouseUseCase`: Sets `archivedAt` timestamp for safe soft-deletes.
- **Infrastructure Adapter**: `WarehouseRepository` maps cleanly between domain `Warehouse` models and `DbWarehouse` JPA entities.
- **API Adapter**: Implemented `WarehouseResourceImpl` conforming to OpenAPI contracts with a JAX-RS `ContainerResponseFilter` ensuring HTTP 201 Created status on creation.

### 4. Task 4 (BONUS): Product-Warehouse-Store Fulfillment (`fulfillment`)
- Introduced a dedicated **Fulfillment Bounded Context** managing the relationships between Products, Stores, and Warehouses.
- **Dedicated Fulfillment Validator**: `FulfillmentValidator` enforces **all 3 strict business constraints**:
  1. **Max 2 Warehouses per Product-Store**: A product at a given store can be fulfilled by at most 2 distinct warehouses.
  2. **Max 3 Warehouses per Store**: A store can be fulfilled by at most 3 distinct warehouses across all products.
  3. **Max 5 Products per Warehouse**: A warehouse can fulfill at most 5 distinct products across the entire network.
- Complete domain model `FulfillmentAssignment`, JPA entity `DbFulfillment`, repository `FulfillmentRepository`, service `FulfillmentService`, and REST API `FulfillmentResource` (`/fulfillment/assign`, `/fulfillment/store/{id}`, `/fulfillment/warehouse/{buCode}`).

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
- **Test Coverage Tracking Document**: [`docs/TEST_COVERAGE_REPORT.md`](docs/TEST_COVERAGE_REPORT.md)
  - Complete package-by-package and class-by-class coverage metrics.

---

## Quality Metrics & Testing

- **Total Tests**: **111 tests** across 12 test suites (0 failures, 0 errors, 0 skipped).
- **Line Coverage**: **86.85%** (436 / 502 lines) — verified by JaCoCo.
- **Instruction Coverage**: **82.78%** (1,735 / 2,096 instructions).
- **Automated Gate**: `jacoco-maven-plugin:check` configured to fail any build dropping below 80% line coverage.
- **Test Profile Portability**: Test execution uses in-memory H2 in PostgreSQL compatibility mode, enabling ultra-fast test execution without external Docker dependencies.

---

## Continuous Integration (CI/CD)

The repository includes a production-ready GitHub Actions workflow in [`.github/workflows/ci.yml`](.github/workflows/ci.yml) that:
1. Triggers on every `push` and `pull_request` to `main` and `master`.
2. Sets up OpenJDK 21 with Maven caching.
3. Runs `./mvnw clean verify`, executing all 111 tests and verifying JaCoCo coverage exceeds 80%.
4. Archives and publishes JaCoCo HTML reports and Surefire test reports as workflow artifacts.

---

## Getting Started

### Prerequisites
- **Java 21+** (e.g., Eclipse Temurin 21)
- **Maven 3.8+** (or use included `./mvnw`)
- *(Optional for Prod)* Docker / PostgreSQL 13+

### Building & Running Tests
To run the full build, execute all 111 tests, and verify code coverage:
```bash
cd fcs-interview-code-assignment-main/java-assignment
./mvnw clean verify
```

To view the generated JaCoCo coverage report:
```bash
open docs/jacoco-report/index.html
```

### Running in Development Mode
```bash
cd fcs-interview-code-assignment-main/java-assignment
./mvnw quarkus:dev
```
Navigate to:
- Application UI: [http://localhost:8080/index.html](http://localhost:8080/index.html)
- Swagger UI: [http://localhost:8080/q/swagger-ui](http://localhost:8080/q/swagger-ui)
- Dev UI: [http://localhost:8080/q/dev-ui](http://localhost:8080/q/dev-ui)

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
