# Architecture & Technical Questions

---

### 1. In this codebase, we have some different implementation strategies when it comes to database access layer and manipulation. If you would maintain this codebase, would you refactor any of those? Why?

**Answer:**

Having worked extensively with both Spring Boot and Quarkus in production microservices, I immediately noticed three distinct persistence strategies coexisting in this codebase:

1. **Active Record Pattern (PanacheEntity on `Store`)**:
   - `Store extends PanacheEntity`, where query and persistence methods are static or instance methods directly on the entity (`Store.findById()`, `store.persist()`).
   - *The Problem*: While convenient for quick prototypes, Active Record is an anti-pattern for maintainable enterprise systems. It tightly couples the persistence mechanism to the domain class, violates the Single Responsibility Principle, and makes standalone unit testing painful. In fact, when writing unit tests for our new `FulfillmentService`, static calls on `Store` would fail outside Quarkus's bytecode transformation unless we booted the entire container or used static mocking.
   - *Refactoring Action*: I refactored `Store` to use a dedicated repository (`StoreRepository implements PanacheRepository<Store>`). This immediately allowed standard Mockito mocking (`when(storeRepo.findById(id)).thenReturn(...)`) and separated data access from entity state.

2. **Repository Pattern (PanacheRepository on `ProductRepository`)**:
   - `ProductRepository implements PanacheRepository<Product>`.
   - *Evaluation*: This is a pragmatic, clean approach for standard CRUD subdomains. It cleanly separates the entity from query logic and is easily injectable via CDI (`@Inject`). For simple entities that don't have complex domain logic, this pattern keeps boilerplate minimal without sacrificing testability.

3. **Hexagonal Architecture / Ports & Adapters (`warehouses` package)**:
   - Pure POJO domain model (`Warehouse`), domain ports (`WarehouseStore`), and an infrastructure adapter (`WarehouseRepository` mapping `Warehouse` $\leftrightarrow$ `DbWarehouse`).
   - *Evaluation*: This is the gold standard for complex core domains. By ensuring `Warehouse` has zero JPA/Hibernate annotations, the business rules in our use cases (`CreateWarehouseUseCase`, `ReplaceWarehouseUseCase`, `ArchiveWarehouseUseCase`) remain completely isolated from database schema migrations, ORM quirks, and framework upgrades.

**My Refactoring Decision:**
If I were maintaining this codebase long-term, I would standardize on two tiers:
- **Core Bounded Contexts with Rich Business Rules** (e.g., `Warehouse`, `Fulfillment`): Full Hexagonal Architecture. Keep domain models pure POJOs, express all database needs through domain ports, and isolate JPA mappings inside adapter repositories.
- **Ancillary CRUD Subdomains** (e.g., `Product`, `Store`): Use the Panache Repository pattern (`PanacheRepository<T>`). Eliminate all Active Record static method usage to maintain uniform testability across the entire engineering team.

---

### 2. When it comes to API spec and endpoint handlers, we have an OpenAPI YAML file for the `Warehouse` API from which we generate code, but for the other endpoints (`Product` and `Store`) we just coded directly everything. What would be your thoughts about what are the pros and cons of each approach, and what would be your choice?

**Answer:**

This represents the classic architectural debate between **Contract-First (Design-First)** and **Code-First** API design. Having led multi-team API platforms, here is how I weigh the trade-offs:

#### Contract-First (Used for `Warehouse` API via `warehouse-openapi.yaml`):
* **Pros**:
  - **API Governance & Single Source of Truth**: The contract is agreed upon and versioned in Git *before* implementation starts. Frontend, mobile, and backend teams align on payloads, error codes, and query params upfront.
  - **Parallel Engineering**: Downstream consumers don't wait for backend code; they generate mock servers (via Prism or WireMock) and client SDKs on day one.
  - **CI Breaking Change Detection**: Tools like `openapi-diff` can run in GitHub Actions to automatically reject pull requests that introduce breaking contract changes.
* **Cons & Real-World Friction**:
  - **Generator Rigidity**: In our project, the Quarkus OpenAPI generator produced JAX-RS interfaces returning 200 OK by default. To adhere to REST best practices and return `201 Created` on warehouse creation without breaking the generated interface, we had to introduce a JAX-RS `ContainerResponseFilter`.
  - **Build Overhead**: Requires code-generation build steps and extra mapping layers between generated DTOs and domain models.

#### Code-First (Used for `Product` and `Store` via JAX-RS annotations):
* **Pros**:
  - **Developer Velocity**: Fast iteration. Developers write standard Java classes with familiar annotations (`@Path`, `@GET`, `@POST`, `@ResponseStatus`).
  - **Full Native Control**: Exact control over response builders, HTTP headers, streaming, and custom status codes without generator workarounds.
  - **Automated Docs**: Tools like Quarkus SmallRye OpenAPI inspect annotations and generate Swagger UI on the fly (`/q/swagger-ui`).
* **Cons & Real-World Friction**:
  - **Schema Drift**: An unreviewed change to a Java getter or field can silently break downstream consumers in production.
  - **Annotation Clutter**: Java classes become cluttered with OpenAPI documentation metadata (`@Operation`, `@APIResponse`, `@Schema`), obscuring the actual business code.

#### My Recommendation for an Enterprise Platform:
For external, public, or cross-team microservice boundaries, **Contract-First is mandatory**. The governance and stability it provides far outweigh the minor generator friction. To make Contract-First pleasant for developers, I configure code generation to produce only DTOs and interface skeletons, and use MapStruct for zero-overhead mapping into domain models.

For small internal services, rapid prototypes, or private BFF (Backend-for-Frontend) APIs, Code-First is acceptable provided automated CI checks export and validate the OpenAPI spec against previous releases.

---

### 3. Given the need to balance thorough testing with time and resource constraints, how would you prioritize and implement tests for this project? Which types of tests would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**

Testing is an investment where return on investment (ROI) is measured in **confidence vs. execution time**. Under realistic delivery deadlines, I apply the Agile Testing Pyramid and risk-based prioritization:

#### 1. Test Prioritization Strategy

- **Tier 1: Fast Domain Unit Tests (70–80% of volume - Highest Priority & ROI)**:
  - *Target*: Pure domain use cases and validation invariants (`CreateWarehouseUseCaseTest`, `ReplaceWarehouseUseCaseTest`, `ArchiveWarehouseUseCaseTest`, `FulfillmentServiceTest`, `LocationGatewayTest`).
  - *Why*: These tests run in milliseconds using plain JUnit 5 + Mockito without starting Quarkus or spinning up databases. They allow us to exhaustively test all edge cases: capacity boundary conditions, matching stock requirements, duplicate BU codes, and fulfillment constraints (max 2 WHs/prod-store, max 3 WHs/store, max 5 prods/WH).
  - *Result*: Developers get instant feedback on their laptops within 1–2 seconds.

- **Tier 2: Persistence & Transactional Slice Tests (15–20% of volume)**:
  - *Target*: Repository queries (`WarehouseRepository`, `FulfillmentRepository`) and asynchronous transactional event observers (`StoreSyncObserverTest` testing `AFTER_SUCCESS`).
  - *Why*: Verifies that SQL queries, foreign keys, and transaction boundaries work as intended.
  - *Our Optimization*: In [`application.properties`](../src/main/resources/application.properties), we configured `%test.quarkus.datasource.db-kind=h2` in PostgreSQL compatibility mode. This allows all 73 integration and component tests to execute in under 14 seconds without waiting for Docker containers to spin up on every build.

- **Tier 3: End-to-End API Component Tests (5–10% of volume)**:
  - *Target*: REST-Assured endpoint tests (`WarehouseEndpointTest`, `StoreEndpointTest`, `ProductEndpointTest`, `FulfillmentEndpointTest`).
  - *Why*: Smoke-tests HTTP serialization, status codes (200, 201, 400, 404), and error mappers from the client's perspective.

#### 2. Ensuring Test Coverage Remains Effective Over Time

- **Automated CI Quality Gates (JaCoCo Enforcement)**:
  High coverage is useless if it degrades with subsequent PRs. We configured `jacoco-maven-plugin:check` in `pom.xml` to strictly fail the build if line coverage drops below **80%**. Our test suite currently achieves **86.98% line coverage** across all packages.
- **Mutation Testing (Pitest)**:
  Line coverage measures execution, not assertion quality. In mature teams, I introduce periodic mutation testing (altering conditionals and return values) to ensure tests genuinely fail when bugs are injected.
- **Architecture Fitness Rules (ArchUnit)**:
  Automated tests ensuring hexagonal boundaries are respected (e.g., verifying that classes in `domain` never import packages from `adapters`, `hibernate`, or `jaxrs`).
- **Consumer-Driven Contract Tests (Pact)**:
  Validates API contracts between microservices in CI without requiring expensive, flaky multi-service staging environments.
