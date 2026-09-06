# Case Study: Fulfillment & Cost Control Architecture

---

## Scenario 1: Cost Allocation and Tracking
**Situation**: The company needs to track and allocate costs accurately across different Warehouses and Stores. The costs include labor, inventory, transportation, and overhead expenses.

**Task**: Discuss the challenges in accurately tracking and allocating costs in a fulfillment environment. Think about what are important considerations for this, what previous experiences you could relate to this problem, and elaborate on key questions and considerations.

### 1. Practical Challenges & Considerations
- **Indirect & Fixed Overhead Absorption**: Distributing fixed facilities costs (rent, property taxes, cooling/heating, IT infrastructure, management salaries) fairly across dynamic, seasonal inventory flows without distorting per-unit profitability.
- **Activity & Labor Variability**: Labor is not uniform. In a past fulfillment network I worked with, receiving a pallet of pre-packaged goods took 3 minutes, whereas picking and packing fragile, hazardous, or non-standard items took 18 minutes per order line. Allocating labor by simple headcount or flat hourly averages severely misprices complex SKUs.
- **Multi-Leg Freight & Transshipments**: When an order cannot be fulfilled from a single warehouse and requires inter-warehouse transfers (transshipment) or split shipments, attributing delivery surcharges and carrier fuel costs to specific stores or orders becomes non-trivial.
- **Shrinkage, Spoilage, and Holding Cost**: Carrying costs fluctuate over time. Perishable or seasonal goods accumulate holding costs that must be attributed correctly between the warehouse custodian and the requesting retail store.

### 2. Architectural & Technical Approach
- **Activity-Based Costing (ABC) Engine**: Transition from flat-rate overheads to a driver-based allocation model:
  - *Storage Driver*: Volume ($\text{m}^3$) $\times$ Duration (Days stored).
  - *Handling Driver*: Labor minutes per pick/pack operation based on SKU handling class.
  - *Transportation Driver*: Weight $\times$ Distance (Ton-kilometers) + carrier accessorial fees.
- **Event-Driven Cost Telemetry**: Emit granular domain events (`ItemReceived`, `InventoryStored`, `OrderPicked`, `ShipmentDispatched`, `DeliveryCompleted`) into an append-only event stream (Kafka). A dedicated Cost Control service consumes these events and updates an audit-logged cost ledger in real time.

### 3. Relevant Real-World Use Case
* **The "Split-Shipment Black Friday" Scenario**: During peak seasonal sales, a customer orders 3 items for store delivery. Item A and B ship from Amsterdam (`AMSTERDAM-001`), but Item C is out of stock there and must be transshipped from Zwolle (`ZWOLLE-001`). 
* *Cost Allocation Solution*: The system captures both warehouse pick events and two separate carrier legs. The transshipment freight is tagged under an "Internal Balancing Cost Center" rather than penalizing the receiving store's local margin, providing leadership with transparent visibility into the exact cost of inventory imbalances.

### 4. Key Questions for Stakeholders Before Defining Scope
1. What costing methodology does the corporate Finance team mandate (Activity-Based Costing, Standard Costing, or Marginal Costing)?
2. At what granularity must costs be reported (real-time per-order attribution vs. daily/monthly batch-allocated aggregates)?
3. How are third-party carrier invoices ingested (automated EDI/API invoices with tracking numbers vs. internal estimated rate cards)?
4. Are retail stores treated as independent profit-and-loss (P&L) centers with internal transfer pricing, or as cost centers within a unified supply chain?

---

## Scenario 2: Cost Optimization Strategies
**Situation**: The company wants to identify and implement cost optimization strategies for its fulfillment operations. The goal is to reduce overall costs without compromising service quality.

**Task**: Discuss potential cost optimization strategies for fulfillment operations and expected outcomes. How would you identify, prioritize, and implement these strategies?

### 1. Concrete Optimization Strategies & Expected Outcomes
- **Intelligent Proximity Routing**: Dynamically route store replenishment orders to the optimal warehouse based on physical distance, real-time inventory availability, and carrier delivery zones.
  * *Expected Outcome*: 10–20% reduction in transportation mileage and fuel surcharges; shorter store delivery lead times.
- **Fulfillment Consolidation & Split-Shipment Reduction**: Strictly enforce grouping rules (such as our bonus assignment constraint: max 2 warehouses per product-store and max 3 warehouses per store overall).
  * *Expected Outcome*: Fewer carrier deliveries at store loading docks, lower packaging material usage, and reduced handling fees.
- **Dynamic Warehouse Re-Slotting**: Position high-turnover / fast-moving SKUs near packing stations and lower rack levels, while reserving higher, remote racks for slow-moving inventory.
  * *Expected Outcome*: 15–25% reduction in picker travel time and warehouse labor expense.
- **Demand Analytics & Safety Stock Balancing**: Prevent stockouts and emergency cross-docking by aligning regional warehouse replenishment thresholds with store-level sales velocity.

### 2. Identification, Prioritization, and Implementation Framework
- **Step 1: Data-Driven Pareto Analysis (80/20 Rule)**: Ingest operational cost data to pinpoint the top 20% of fulfillment routes, categories, or warehouses that drive 80% of total operational expenditure.
- **Step 2: Impact vs. Effort Prioritization**:
  - *Quick Wins (High Impact, Low Effort)*: Order batching rules, enforcing minimum store delivery thresholds, carrier rate-card contract audits.
  - *Strategic Initiatives (High Impact, High Effort)*: Automated warehouse sorting equipment, multi-echelon inventory optimization algorithms, warehouse layout re-slotting.
- **Step 3: Phased Rollout & Validation**: Deploy routing algorithm adjustments behind feature flags. Run A/B champion/challenger experiments across a subset of regional stores and monitor key operational KPIs: Cost-per-Order (CPO), Split-Shipment Ratio, and On-Time In-Full (OTIF) delivery rate.

### 3. Relevant Real-World Use Case
* **Seasonal Furniture Re-Slotting**: Ahead of a major summer patio furniture campaign, historical analysis showed outdoor sets accounted for 45% of pick volume but were stored in remote warehouse aisles, causing picker congestion. By dynamically re-slotting outdoor inventory into prime pick aisles 3 weeks prior to promotion, picker walking distance was cut by 22%, saving hundreds of overtime labor hours during peak demand.

### 4. Key Questions for Stakeholders Before Defining Scope
1. What are the contractual Service Level Agreements (SLAs) with retail stores (e.g., guaranteed delivery windows, maximum replenishment turnaround)?
2. What is our current baseline Cost-per-Fulfilled-Unit (CPFU) across each operational facility?
3. What are the physical constraints of our warehouses (e.g., dock door limits, floor load capacity, cold-chain limitations)?
4. Does executive management prioritize immediate cash-flow reductions or long-term structural efficiency improvements?

---

## Scenario 3: Integration with Financial Systems
**Situation**: The Cost Control Tool needs to integrate with existing financial systems to ensure accurate and timely cost data. The integration should support real-time data synchronization and reporting.

**Task**: Discuss the importance of integrating the Cost Control Tool with financial systems. What benefits would the company have, and how would you ensure seamless integration and data synchronization?

### 1. Strategic Importance & Business Value
- **Single Source of Financial Truth**: Synchronizing physical inventory movements with the General Ledger (GL) ensures that balance sheets accurately reflect inventory asset value, eliminating massive month-end inventory write-offs.
- **Near-Real-Time Margin Visibility**: Empowers leadership to identify unprofitable fulfillment routes or cost-leaking warehouses immediately, rather than discovering them 30 days later during month-end book closing.
- **Statutory & Audit Compliance**: Provides unbroken audit trails from physical item movement to journal ledger entries, satisfying SOX, IFRS, and GAAP compliance requirements.

### 2. Architectural Patterns for Reliable Data Synchronization
- **Asynchronous Decoupling via Transactional Outbox Pattern**:
  * *Critical Rule*: Never invoke external financial ERP APIs (e.g., SAP S/4HANA, Oracle NetSuite) synchronously inside the fulfillment request thread. Network timeouts or ERP maintenance windows would degrade or halt fulfillment operations.
  * *Solution*: In the same local DB transaction that updates fulfillment records, write an event payload to an `outbox_events` table. An asynchronous relay (or Debezium CDC) publishes these events to Apache Kafka for consumption by the financial integration layer.
- **Idempotency & Deduplication**: Assign an immutable idempotency key (`businessUnitCode_txId_timestamp`) to every financial message. The receiving ERP adapter uses this key to reject duplicate journal entries if network retries occur.
- **Anti-Corruption Layer (ACL) & Canonical Model**: Implement an ACL service that translates internal logistics concepts (store IDs, unit quantities, pick batches) into standard corporate accounting schemas (Cost Centers, Profit Centers, Debits, Credits).
- **Automated Nightly Reconciliation**: Run automated reconciliation batch jobs at midnight comparing operational ledger totals against ERP general ledger postings, raising high-priority alerts on variances $> 0.01\%$.

### 3. Relevant Real-World Use Case
* **ERP Network Partition During Month-End Close**: During peak month-end order processing, the corporate SAP ERP gateway suffered an unexpected 45-minute outage. Because our system utilized the Transactional Outbox pattern on Kafka rather than synchronous REST calls, zero warehouse operations were blocked. All cost journal entries queued safely and were processed with verified idempotency as soon as SAP recovered, resulting in zero data loss and zero manual finance reconciliations.

### 4. Key Questions for Stakeholders Before Defining Scope
1. What target financial ERP system is in place (SAP S/4HANA, NetSuite, Microsoft Dynamics 365), and what integration protocols does it support (REST APIs, Kafka event streams, sFTP batch EDI/IDoc)?
2. What is the business definition of "real-time" (sub-second event streaming vs. 5-minute micro-batches vs. hourly sync)?
3. What is the exception handling workflow when the financial system rejects a posting (e.g., invalid cost center code or closed fiscal period)?
4. How are multi-currency conversions and cross-border tax jurisdictions handled across regional fulfillment networks?

---

## Scenario 4: Budgeting and Forecasting
**Situation**: The company needs to develop budgeting and forecasting capabilities for its fulfillment operations. The goal is to predict future costs and allocate resources effectively.

**Task**: Discuss the importance of budgeting and forecasting in fulfillment operations and what you would take into account designing a system to support accurate budgeting and forecasting.

### 1. Strategic Importance & Business Value
- **Capacity & Resource Contracting**: Forecasting demand lets logistics directors pre-contract third-party carrier capacity and schedule warehouse shift labor months in advance of holiday rushes, avoiding expensive emergency spot rates and overtime penalties.
- **Capital Expenditure (CapEx) Planning**: Provides data-driven justification for when an existing facility is approaching capacity limit and requires physical expansion or automation investments.
- **Proactive Variance Management**: Comparing budgeted run-rates against actual weekly telemetry enables operational managers to catch cost overruns early.

### 2. Key System Design Considerations
- **Driver-Based Modeling vs. Linear Spend Extrapolation**: Do not simply extrapolate historical dollar spending. Instead, model costs on physical operational drivers:
  $$\text{Forecasted Cost} = \sum (\text{Order Volume} \times \text{Units/Order} \times \text{Pick Rate}) + (\text{Freight Ton-Km} \times \text{Fuel Index}) + \text{Fixed Overhead}$$
- **Time-Series Machine Learning Pipelines**: Pipeline historical operational data into time-series forecasting models (e.g., Prophet, ARIMA, or LightGBM) that ingest promotional calendars, seasonal spikes, store openings, and macroeconomic indices.
- **"What-If" Scenario Simulation Engine**: Provide finance managers with interactive simulation tools to test scenarios (e.g., *"What if carrier diesel fuel surcharges increase by 15%?"* or *"What if 4 new stores open in Region North next quarter?"*).
- **Rolling Forecasts**: Support continuous 12-month rolling forecasts updated monthly as actual demand figures materialize, rather than relying on static, once-a-year annual budgets.

### 3. Relevant Real-World Use Case
* **Pre-Holiday Carrier Capacity Lock-In**: By running a driver-based rolling forecast in August factoring in a 25% projected e-commerce increase for regional stores, our operations team identified that local warehouse capacity would peak at 94% in November. Armed with this forecast, logistics contracted regional carrier capacity 3 months early at standard contractual rates, avoiding a 35% holiday spot-market surcharge and saving significant freight expense.

### 4. Key Questions for Stakeholders Before Defining Scope
1. What planning horizon and review frequency are required (weekly operational scheduling vs. monthly rolling forecasts vs. annual strategic plans)?
2. What external data feeds are available (marketing promotional calendars, macro commodity/fuel indices, regional retail expansion roadmaps)?
3. What forecast accuracy metric (e.g., MAPE $< 5\%$) is considered acceptable, and what variance threshold mandates a formal budget revision?
4. Who are the primary user personas (financial analysts, VP of Supply Chain, regional warehouse facility managers)?

---

## Scenario 5: Cost Control in Warehouse Replacement
**Situation**: The company is planning to replace an existing Warehouse with a new one. The new Warehouse will reuse the Business Unit Code of the old Warehouse. The old Warehouse will be archived, but its cost history must be preserved.

**Task**: Discuss the cost control aspects of replacing a Warehouse. Why is it important to preserve cost history, and how does this relate to keeping the new Warehouse operation within budget?

### 1. Importance of Preserving Cost History & Budgetary Control
- **Performance Baseline & ROI Verification**: Modernizing or relocating a warehouse involves significant capital outlay. Preserving the decommissioned warehouse's historical cost profile (cost per square foot, cost per picked unit, utility consumption) provides the baseline against which the new facility's return on investment (ROI) is measured.
- **Budget Justification & Target Calibration**: The business case for the replacement committed to operational savings (e.g., 20% lower labor cost due to automated conveyors). Historical data confirms whether those savings are actually materializing or if teething issues are driving cost overruns.
- **Audit, Tax, and Asset Depreciation Compliance**: Even after a warehouse is decommissioned, its historical cost data must remain queryable for financial audits, corporate tax depreciation schedules, and lease liability accounting (IFRS 16).
- **Continuity of Business Unit Reporting**: Reusing the Business Unit Code (`businessUnitCode`) allows executive reporting dashboards to track the performance of the geographic market over a multi-year horizon without breaking historical charts.

### 2. Architectural & Execution Controls
- **Dual-Key Data Modeling**:
  * In the operational database and cost ledger, distinguish between facility generations.
  * While `businessUnitCode` (e.g., `MWH.001`) is shared, every physical record must carry an immutable surrogate key (`id`) and timestamp boundaries (`createdAt`, `archivedAt`). Queries for active operations filter on `archivedAt IS NULL`, while cost queries span all generations.
- **Dedicated Transition / Migration Cost Center**:
  * During the replacement transition, both facilities often run simultaneously for 30–90 days (dual-run phase) while inventory is physically relocated and staff are trained.
  * *Crucial Rule*: All dual-run transition expenses (redundant labor, double rent, transfer freight) must be booked to a separate "Warehouse Replacement Project Cost Center" rather than artificially inflating the operational run-rate of either facility.
- **Decommissioning Cost Tracking**: Accurately capture lease surrender penalties, equipment dismantling, environmental disposal, and salvage revenue against decommissioning reserves.

### 3. Relevant Real-World Use Case
* **Automated Facility Migration in Tilburg**: Replacing an aging facility (`MWH.023`) in Tilburg with a mechanized distribution center in the same logistics park. By isolating migration costs (equipment relocation, dual leases) into a separate project code and preserving 3 years of old facility pick-cost history, the company demonstrated within 6 months that the new facility achieved a 24% reduction in cost per picked unit, validating the initial capital expenditure investment to the board.

### 4. Key Questions for Stakeholders Before Defining Scope
1. How long is the dual-run transition period, and will inventory be migrated in phased batches or a single cutoff weekend?
2. How should migration and decommissioning expenses be accounted for (capitalized as CapEx vs. expensed as one-time project OpEx)?
3. Will existing vendor and carrier contracts transfer to the new facility or be renegotiated with new terms?
4. What specific efficiency hurdles (e.g., 15% reduction in cost per dispatched pallet within 6 months) are required to deem the project a financial success?
