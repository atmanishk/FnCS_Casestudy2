# Questions

Here we have 3 questions related to the code base for you to answer. It is not about right or wrong, but more about what's the reasoning behind your decisions.

1. In this code base, we have some different implementation strategies when it comes to database access layer and manipulation. If you would maintain this code base, would you refactor any of those? Why?

**Answer:**
```txt
In this codebase, we observe three distinct database access and manipulation patterns:

1. Active Record Pattern (PanacheEntity):
   - Used by: `Store` (`public class Store extends PanacheEntity`)
   - Characteristics: Database queries and state persistence are directly exposed as static and instance methods on the entity (`Store.findById()`, `Store.find()`, `store.persist()`).
   - Drawbacks & Challenges:
     * Tight Coupling: Directly couples domain entities with the database schema and Hibernate/Panache lifecycle.
     * Testability Barrier: Testing requires bytecode transformation or Quarkus runtime. Plain POJO unit tests fail on static methods (`Store.findById`), making isolated testing awkward or dependent on static mocking tools (QuarkusPanacheMock).
     * Violates Separation of Concerns: Domain models are bloated with JPA mapping annotations (`@Entity`, `@Table`, `@Column`).

2. Repository Pattern (PanacheRepository):
   - Used by: `ProductRepository implements PanacheRepository<Product>` and our newly added `StoreRepository`.
   - Characteristics: Persistence logic and queries are encapsulated in an injectable CDI bean (`@ApplicationScoped`).
   - Advantages: Provides clear separation between the entity and data access operations, simplifies dependency injection, and allows standard Mockito mocking (`when(repo.findById(...))`) in isolated unit tests without requiring a booted database.

3. Hexagonal Architecture / Ports and Adapters (Domain vs. Database Decoupling):
   - Used by: `warehouses` domain (`WarehouseStore` port, `DbWarehouse` JPA entity, and `WarehouseRepository` adapter).
   - Characteristics: The core domain model (`Warehouse`) is a pure POJO completely free of database/ORM annotations. All persistence operations are defined via the `WarehouseStore` domain port, and the database adapter (`WarehouseRepository`) maps between the domain model and the database entity (`DbWarehouse`).
   - Advantages:
     * Domain Purity & DDD: Business logic and validation in use cases (`CreateWarehouseUseCase`, `ReplaceWarehouseUseCase`, `ArchiveWarehouseUseCase`) are completely isolated from schema changes, ORM bugs, and framework upgrades.
     * High-Speed Deterministic Testing: Use case unit tests execute in milliseconds without starting Quarkus or spinning up test databases, relying solely on mock implementations of the domain port.

Refactoring Proposal:
If maintaining this codebase, I would refactor the persistence strategies as follows:
1. Eliminate Active Record on `Store`: Refactor `Store` to use `StoreRepository` (or full Hexagonal Ports/Adapters). Active Record's static method reliance creates technical debt and complicates testing as business rules evolve.
2. Adopt Hexagonal Ports and Adapters for Complex Core Domains: Keep pure POJO domain models and ports for core domains with rich invariants (like `Warehouse` and `Fulfillment`).
3. Keep Panache Repositories for Simple CRUD Subdomains: For simple CRUD subdomains (such as `Product`), Panache Repositories (`PanacheRepository<Product>`) offer a pragmatic balance of separation of concerns and developer ergonomics without excessive boilerplate.
4. Separate Domain Models from JPA Entities: Avoid persisting domain models directly. Maintaining distinct persistence entities (`DbWarehouse`, `DbStore`) ensures schema migrations and database constraints never bleed into or restrict domain business logic.
```
----
2. When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded directly everything. What would be your thoughts about what are the pros and cons of each approach and what would be your choice?

**Answer:**
```txt
This compares Contract-First (Design-First / Spec-First) with Code-First (Implementation-First) API development:

1. Contract-First (OpenAPI Spec -> Code Generation) [Warehouse API]:
   Pros:
   - Single Source of Truth & Clear Contract: The API specification (`warehouse-openapi.yaml`) acts as the definitive contract agreed upon by frontend, mobile, partner, and backend teams before implementation begins.
   - Parallel Engineering: Consumers can generate mock servers (e.g., WireMock, Prism) and client SDKs immediately, enabling frontend/integration teams to work concurrently without waiting for backend completion.
   - API Governance & Backward Compatibility: Spec changes can be tracked in version control and analyzed via automated CI linting (e.g., Spectral) and breaking-change detection tools (e.g., openapi-diff) before deployment.
   - Framework Agnostic: The contract is not coupled to Java or Quarkus; it can generate clients and stubs in TypeScript, Go, Python, etc.

   Cons:
   - Tooling Friction: Code generators sometimes produce rigid interfaces or suboptimal method signatures (e.g., Quarkus OpenAPI generator generating JAX-RS interfaces with default 200 OK responses, necessitating response filters for 201 Created).
   - Build-Time Overhead: Requires code generation build steps (`mvn compile`) and synchronization between YAML files and generated classes.
   - Developer Context Switching: Developers must write YAML schemas and types rather than native language constructs.

2. Code-First (Direct Java / JAX-RS Coding) [Product & Store APIs]:
   Pros:
   - High Developer Velocity & Simplicity: Developers write standard Java classes with familiar annotations (`@Path`, `@GET`, `@POST`, `@Consumes`, `@Produces`). No code generators, plugins, or schema synchronizations required.
   - Expressiveness & Flexibility: Full native control over response building (`Response.status(201).entity(...).build()`), custom headers, streaming, and exception mappers.
   - Automatic Spec Generation: Quarkus SmallRye OpenAPI can inspect JAX-RS annotations and generate OpenAPI specifications and Swagger UI automatically.

   Cons:
   - Schema Drift & Breaking Changes: Minor code or model changes can unintentionally change JSON serialization or HTTP status codes without explicit API contract review.
   - Consumer Blocking: Frontend and external consumers are blocked until the backend models and endpoints are coded and deployed.
   - Code Annotation Bloat: Adding comprehensive OpenAPI documentation directly in Java code (`@Operation`, `@APIResponse`, `@Parameter`, `@Schema`) clutters domain code with presentation metadata.

My Decision / Recommendation:
For an enterprise fulfillment platform:
- Core Platform & External/Consumer APIs: Contract-First (OpenAPI-First).
  In microservices architectures with distributed teams, the contract-first approach is essential for API governance, preventing regression breakages, and enabling client generation.
  * To solve generator rigidity: Generate only DTO models and interface contracts. Use mapping libraries (e.g., MapStruct) to cleanly map between OpenAPI DTOs and internal domain models.
- Internal Microservice-to-Microservice or Rapid Prototypes: Code-First can be accepted in early stages, provided automated CI tools export and validate OpenAPI specs against contract diffs.
```
----
3. Given the need to balance thorough testing with time and resource constraints, how would you prioritize and implement tests for this project? Which types of tests would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**
```txt
To balance comprehensive testing with time and resource constraints, I apply the Agile Testing Pyramid and Risk-Based Prioritization:

1. Test Prioritization Hierarchy:

   A. Fast Unit Tests (70-80% of test suite - Highest Priority):
      - Target: Pure domain use cases and business rules (`CreateWarehouseUseCaseTest`, `ReplaceWarehouseUseCaseTest`, `ArchiveWarehouseUseCaseTest`, `FulfillmentServiceTest`, `LocationGatewayTest`).
      - Strategy: Test core business invariants (e.g., capacity rules, warehouse replacement archiving, store-product assignment constraints).
      - Implementation: Standalone JUnit 5 + Mockito tests without booting Quarkus or spinning up databases.
      - ROI: Near-instant execution (milliseconds), high deterministic coverage of edge cases, and immediate feedback in local development and CI.

   B. Integration / Persistence Slice Tests (15-20% of test suite):
      - Target: Repository implementations (`WarehouseRepository`, `FulfillmentRepository`, `StoreRepository`) and asynchronous / transactional event observers (`StoreSyncObserver` with `TransactionPhase.AFTER_SUCCESS`).
      - Strategy: Validate SQL queries, schema constraints, transactional rollbacks, and event bus integrations.
      - Implementation: Use Quarkus test profile (`@QuarkusTest`) paired with an embedded/fast in-memory database (H2 in PostgreSQL compatibility mode) for lightning-fast CI builds, supplemented with Testcontainers PostgreSQL in nightly builds to verify dialect-specific queries.

   C. API / Component Tests (5-10% of test suite):
      - Target: End-to-end HTTP resource flows (`WarehouseEndpointTest`, `StoreEndpointTest`, `ProductEndpointTest`, `FulfillmentEndpointTest`).
      - Strategy: Validate HTTP status codes (200, 201, 400, 404), JSON serialization/deserialization, validation filters, and error handlers using REST-Assured.

2. Ensuring Effective Test Coverage Over Time:

   - Automated CI Quality Gates (JaCoCo Enforcement):
     Configure `jacoco-maven-plugin:check` to fail pull requests in GitHub Actions if line coverage drops below 80% (or misses critical branches). This prevents coverage degradation as features are added.
   
   - Mutation Testing (Pitest):
     Line coverage alone does not prove test quality. Periodic mutation testing (e.g., modifying conditionals, boundary checks, and return values) verifies that assertions genuinely catch bugs and prevents "assertion-free" tests.

   - Architecture Fitness Tests (ArchUnit):
     Enforce hexagonal architecture boundaries via automated tests (e.g., verifying that domain packages never import infrastructure, JPA, or REST framework packages).

   - Contract Testing (Pact / OpenAPI Spec Diff):
     Run automated schema compatibility checks in CI to verify that API responses conform to the OpenAPI contract, preventing regressions for downstream consumers without needing complex multi-service end-to-end environments.
```
