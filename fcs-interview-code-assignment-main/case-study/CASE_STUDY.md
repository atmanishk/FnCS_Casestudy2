# Case Study Scenarios to Discuss

## Scenario 1: Cost Allocation and Tracking
**Situation**: The company needs to track and allocate costs accurately across different Warehouses and Stores. The costs include labor, inventory, transportation, and overhead expenses.

**Task**: Discuss the challenges in accurately tracking and allocating costs in a fulfillment environment. Think about what are important considerations for this, what are previous experiences that you have you could related to this problem and elaborate some questions and considerations.

**Questions you may have and considerations:**
```txt
1. Core Challenges in Fulfillment Cost Tracking:
   - Shared Overhead Distribution: Distributing fixed costs (warehouse lease, utilities, IT infrastructure, management salaries) fairly across millions of dynamic, heterogeneous inventory units and store deliveries.
   - Activity & Labor Variability: Labor cost per unit is not uniform; receiving pallets, hazardous/fragile material handling, individual item picking, and custom store packing carry vastly different cycle times.
   - Multi-Leg Transportation & Transshipments: When orders are fulfilled from multiple warehouses or require inter-warehouse transfers, allocating freight charges and fuel surcharges to a specific store or product unit is complex.
   - Shrinkage, Spoilage, and Holding Costs: Perishable or obsolete goods accumulate carrying costs that fluctuate over time and must be attributed correctly between the warehouse custodian and the receiving store.

2. Important Technical & Architectural Considerations:
   - Activity-Based Costing (ABC) Model: Move beyond flat overhead percentages to driver-based allocation (e.g., storage cost driven by cubic meters/day, picking cost driven by labor minutes per pick, freight driven by weight and distance).
   - Event-Driven Operational Telemetry: Capture cost drivers at the source by emitting domain events (`InventoryReceived`, `ItemStored`, `OrderPicked`, `ShipmentDispatched`, `DeliveryCompleted`) containing operational attributes (weight, volume, labor timestamps, carrier tier) into an append-only event ledger.
   - Entity Hierarchy & Granularity: Clearly define the unit of cost attribution (SKU level, order level, store level, or warehouse business unit level) to prevent data explosion while preserving auditability.

3. Key Questions to Gather Before Defining Boundaries:
   - What costing methodology does the Finance team mandate (Activity-Based Costing, Standard Costing, or Marginal Costing)?
   - At what granularity must costs be reported (real-time per-order cost vs. daily/monthly batch-allocated aggregates)?
   - How are third-party carrier costs ingested (pre-calculated automated EDI/API invoices vs. internal estimated rate cards)?
   - Are stores treated as independent profit-and-loss (P&L) centers charged internal transfer prices, or as cost centers within a unified supply chain?
```

## Scenario 2: Cost Optimization Strategies
**Situation**: The company wants to identify and implement cost optimization strategies for its fulfillment operations. The goal is to reduce overall costs without compromising service quality.

**Task**: Discuss potential cost optimization strategies for fulfillment operations and expected outcomes from that. How would you identify, prioritize and implement these strategies?

**Questions you may have and considerations:**
```txt
1. Potential Cost Optimization Strategies & Expected Outcomes:
   - Intelligent Order Routing & Proximity Sourcing: Route store replenishment orders to the optimal warehouse based on distance and available stock.
     * Expected Outcome: 10-20% reduction in transportation and fuel expenses, faster store replenishment.
   - Split-Shipment & Multi-Warehouse Consolidation: Enforce strict fulfillment grouping constraints (such as our bonus assignment constraint: max 2 warehouses per product-store and max 3 warehouses per store).
     * Expected Outcome: Reduced packaging materials, fewer dock deliveries, and lower handling fees.
   - Dynamic Warehouse Slotting: Place high-velocity / fast-moving SKUs near loading docks and packing stations, reserving higher racks for slow-moving inventory.
     * Expected Outcome: 15-25% reduction in picker travel time and warehouse labor costs.
   - Automated Replenishment & Safety Stock Tuning: Use demand analytics to balance stock levels across regional warehouses, preventing expensive inter-warehouse emergency transfers and stockouts.

2. Identification, Prioritization, and Implementation Framework:
   - Step 1: Identification (Data-Driven Pareto Analysis): Ingest operational cost data to identify the "vital few"—the 20% of fulfillment routes or product categories driving 80% of costs (typically freight and labor).
   - Step 2: Prioritization (Impact vs. Effort Matrix):
     * Quick Wins (High Impact, Low Effort): Consolidation rules in the routing engine, rate-card contract audits, order batching.
     * Strategic Initiatives (High Impact, High Effort): Warehouse layout re-slotting, multi-echelon inventory optimization algorithms, automated sorting systems.
   - Step 3: Phased Implementation & Validation:
     * Deploy algorithmic changes behind feature toggles.
     * Run champion/challenger A/B testing on a subset of stores/warehouses.
     * Track key KPIs: Cost-per-Order (CPO), Split-Shipment Ratio, On-Time In-Full (OTIF) delivery rate.

3. Key Questions to Gather Before Defining Boundaries:
   - What are the contractual Service Level Agreements (SLAs) with stores (e.g., maximum lead time, mandatory delivery windows)?
   - What is the current baseline Cost-per-Fulfilled-Unit (CPFU) across all operational units?
   - What are the physical constraints of each warehouse (e.g., dock door limits, cold chain requirements, floor load capacity)?
   - Does management prioritize short-term cash flow savings or long-term structural efficiency improvements?
```

## Scenario 3: Integration with Financial Systems
**Situation**: The Cost Control Tool needs to integrate with existing financial systems to ensure accurate and timely cost data. The integration should support real-time data synchronization and reporting.

**Task**: Discuss the importance of integrating the Cost Control Tool with financial systems. What benefits the company would have from that and how would you ensure seamless integration and data synchronization?

**Questions you may have and considerations:**
```txt
1. Strategic Importance & Business Benefits:
   - Single Source of Financial Truth: Eliminates discrepancies between physical inventory/fulfillment operations and General Ledger (GL) accounts, preventing costly month-end reconciliation adjustments.
   - Near Real-Time Profitability Visibility: Enables executive leadership to track gross margins and operational expenditure (OpEx) dynamically, identifying unprofitable routes or cost leaks immediately.
   - Regulatory Compliance & Audit Readiness: Ensures end-to-end traceability of inventory asset valuations for tax compliance, SOX controls, and standard accounting principles (GAAP/IFRS).

2. Architectural Patterns for Seamless Integration & Synchronization:
   - Asynchronous Decoupling via Transactional Outbox Pattern: Never couple operational fulfillment APIs directly to external financial ERPs (e.g., SAP, Oracle NetSuite) via synchronous REST calls. Instead, save financial ledger events in the local database within the same transaction, then publish via an event broker (e.g., Apache Kafka / Debezium) to ensure zero message loss even if the financial ERP experiences downtime.
   - Idempotency & Deduplication: Assign an immutable idempotency key (`businessUnitCode_eventRef_timestamp`) to every financial message. The receiving adapter or financial gateway must deduplicate requests to prevent duplicate GL postings.
   - Canonical Data Model (CDM) & Anti-Corruption Layer (ACL): Implement an ACL that transforms internal fulfillment domain concepts into standard accounting schemas (Cost Centers, Profit Centers, Debits, Credits).
   - Automated Reconciliation & Anomaly Detection: Schedule automated reconciliation reconciliation jobs (e.g., daily midnight runs) comparing operational fulfillment totals against ERP journal entries, alerting on any variance exceeding defined thresholds.

3. Key Questions to Gather Before Defining Boundaries:
   - What target financial system is in place (e.g., SAP S/4HANA, NetSuite, Microsoft Dynamics), and what integration protocols are available (REST APIs, Kafka streams, sFTP batch EDI/IDoc)?
   - What is the strict business definition of "real-time" (sub-second streaming vs. 5-minute micro-batches vs. hourly sync)?
   - What is the exception resolution workflow when a financial transaction is rejected due to invalid cost center codes or closed fiscal periods?
   - How are currency conversions and multi-entity tax jurisdictions managed across cross-border fulfillment networks?
```

## Scenario 4: Budgeting and Forecasting
**Situation**: The company needs to develop budgeting and forecasting capabilities for its fulfillment operations. The goal is to predict future costs and allocate resources effectively.

**Task**: Discuss the importance of budgeting and forecasting in fulfillment operations and what would you take into account designing a system to support accurate budgeting and forecasting?

**Questions you may have and considerations:**
```txt
1. Strategic Importance:
   - Capacity and Resource Planning: Allows operations leaders to contract carrier freight capacity and schedule labor shifts well in advance of peak seasons (e.g., holiday rushes, Black Friday), avoiding premium spot-market rates and overtime penalties.
   - Capital Expenditure (CapEx) Planning: Provides data to justify when a warehouse is nearing capacity limit and a new facility or expansion is required.
   - Proactive Variance Analysis: Comparing budgeted run-rates with actuals on a weekly basis enables managers to detect cost anomalies and adjust operations before quarterly budgets are breached.

2. System Design Considerations:
   - Driver-Based Modeling: Base forecasts on physical business drivers (forecasted order volume, units per order, SKU dimensions, fuel price indices, seasonal wage rates) rather than static historical dollar spending.
   - Time-Series Forecasting & Machine Learning: Integrate time-series predictive models (e.g., Prophet, ARIMA, or gradient-boosted trees) trained on historical order volumes, seasonal promotional calendars, and macroeconomic factors.
   - "What-If" Scenario Simulation: Allow financial planners to run simulations (e.g., "What if carrier fuel surcharges rise 12%?" or "What if 3 new retail stores open in Region East next quarter?").
   - Rolling Forecasts vs. Static Annual Budgets: Support continuous 12-month rolling forecasts updated monthly as actual demand data arrives.
   - Granular Tagging: Capture budget vs. actual across dimensions: Business Unit Code, Store Identifier, Cost Category (Labor, Freight, Facilities), and Time Bucket.

3. Key Questions to Gather Before Defining Boundaries:
   - What planning horizon and review frequency does management require (weekly operational forecasts vs. monthly rolling budgets vs. annual plans)?
   - What external data inputs are accessible (promotional schedules from marketing, macroeconomic indices, carrier contractual rate cards)?
   - What level of forecast accuracy (e.g., Mean Absolute Percentage Error < 5%) is expected, and what variance threshold triggers a mandatory budget revision?
   - Who are the primary users of the tool (finance analysts, warehouse operational directors, regional logistics managers)?
```

## Scenario 5: Cost Control in Warehouse Replacement
**Situation**: The company is planning to replace an existing Warehouse with a new one. The new Warehouse will reuse the Business Unit Code of the old Warehouse. The old Warehouse will be archived, but its cost history must be preserved.

**Task**: Discuss the cost control aspects of replacing a Warehouse. Why is it important to preserve cost history and how this relates to keeping the new Warehouse operation within budget?

**Questions you may have and considerations:**
```txt
1. Importance of Preserving Cost History & Budgetary Control:
   - Performance Baseline & Efficiency Benchmarking: The cost history of the archived warehouse (cost per square foot, cost per picked unit, utility expenses) provides the baseline against which the new facility’s efficiency and return on investment (ROI) are measured.
   - Budget Target Justification: Replacing a warehouse requires substantial capital investment. Historical data proves whether promised operational efficiencies (e.g., 20% lower labor cost due to better layout) are being achieved or if unexpected cost overruns are occurring.
   - Regulatory, Tax, and Asset Depreciation Auditing: The old facility's historical costs remain subject to financial audits, tax write-downs, lease liability evaluations, and asset depreciation schedules even after operational shutdown.
   - Continuity of Bounded Context Reporting: Reusing the Business Unit Code allows business reporting to maintain historical continuity across the regional unit while clearly demarcating performance before and after modernization.

2. Cost Control Execution & Architectural Safeguards:
   - Dual-Key Data Modeling: The operational database and cost control system must distinguish between facility versions. While the Business Unit Code is shared, every record must incorporate an immutable surrogate identifier or version key (e.g., `buCode` + `version` or `facility_id`), preventing cost records of the old warehouse from being overwritten or commingled with the new facility.
   - Separate Migration & Transition Cost Center: During the replacement transition, both warehouses often operate concurrently (dual-run phase) while inventory is relocated and systems are tested. Transition costs (double rent, redundant labor, transfer freight) must be isolated into a "Transition Project Cost Center" rather than artificially inflating the operational run-rate of either warehouse.
   - Decommissioning Cost Accruals: Track dismantling, lease termination, environmental cleanup, and asset salvage values against specific decommissioning provisions.

3. Key Questions to Gather Before Defining Boundaries:
   - How long will the dual-run/transition period last, and will inventory be migrated all at once or in phased batches?
   - How should migration and setup costs be classified (capitalized as CapEx vs. expensed as one-time project OpEx)?
   - Are existing supplier and logistics contracts transferring to the new warehouse, or will new rates take effect upon opening?
   - What specific cost savings or productivity targets were committed in the capital expenditure business case for the replacement?
```

## Instructions for Candidates
Before starting the case study, read the [BRIEFING.md](BRIEFING.md) to quickly understand the domain, entities, business rules, and other relevant details.

**Analyze the Scenarios**: Carefully analyze each scenario and consider the tasks provided. To make informed decisions about the project's scope and ensure valuable outcomes, what key information would you seek to gather before defining the boundaries of the work? Your goal is to bridge technical aspects with business value, bringing a high level discussion; no need to deep dive.
