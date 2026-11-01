# SupplyChain OS

**AI-Powered Supply Chain Optimization & Automated Dispatch System.** Academic Major Project-1 MVP.

A customer places an order. The system checks stock in every warehouse, rejects those that can't supply the whole order, scores the rest on distance, delivery cost, inventory and priority, then selects one, reserves the stock, confirms the order and creates a dispatch. Every decision is stored and can be replayed on a map.

> **What "AI" means in this version.** The warehouse decision is a deterministic, rule-based weighted score. This MVP contains no machine learning, forecasting or LLM. Payments and shipment movement are simulated; there is no real payment gateway, courier or GPS.

| Layer | Technology |
|---|---|
| Frontend | Next.js (App Router), TypeScript, Tailwind CSS, Zustand, Framer Motion, PixiJS, Recharts |
| Backend | Java, Spring Boot, Spring Security with JWT, Spring Data JPA, Flyway, OpenAPI |
| Database | MySQL 8 |

More detail: [architecture](docs/ARCHITECTURE.md), [specification](docs/SPECIFICATION.md), [progress checklist](docs/CHECKLIST.md), [backend](backend/README.md), [frontend](frontend/README.md).

## Setup

You need a JDK (21 or newer), Node.js and MySQL 8.

**1. Database and settings**

```bash
cd backend
cp .env.example .env     # then fill it in; see below
./local-db.sh start      # a private MySQL on port 3307, needing no root access
```

Minimum `.env` for the demonstration data. These are development values, and `.env` is ignored by git:

```properties
DB_URL=jdbc:mysql://127.0.0.1:3307/supplychain
DB_TEST_URL=jdbc:mysql://127.0.0.1:3307/supplychain_test
DB_PASSWORD=choose-a-password
JWT_SECRET=at-least-32-bytes-for-example-from-openssl-rand-base64-48
SEED_ADMIN_EMAIL=admin@demo.local
SEED_ADMIN_PASSWORD=Admin@12345
SEED_CUSTOMER_PASSWORD=Customer@12345
```

To use an installed MySQL instead, see the [backend README](backend/README.md).

**2. Backend**, on port 8081. The first start on an empty database creates the schema and all demonstration data.

```bash
cd backend
./mvnw spring-boot:run
```

**3. Frontend**, on port 3000

```bash
cd frontend
npm install
npm run dev              # or, for the demonstration: npm run build && npm start
```

Open <http://localhost:3000>. API documentation is at <http://localhost:8081/swagger-ui.html>.

## Demonstration accounts

For local development only. Never use these on a deployed system.

| Role | Email | Password | Delivery city |
|---|---|---|---|
| Administrator | `admin@demo.local` | `Admin@12345` | |
| Customer | `asha.rao@demo.local` | `Customer@12345` | Davanagere |
| Customer | `rahul.kulkarni@demo.local` | `Customer@12345` | Belagavi |
| Customer | `priya.nair@demo.local` | `Customer@12345` | Mangaluru |
| Customer | `vikram.shetty@demo.local` | `Customer@12345` | Udupi |
| Customer | `meera.patil@demo.local` | `Customer@12345` | Hubli |
| Customer | `arjun.gowda@demo.local` | `Customer@12345` | Mysuru |
| Customer | `kavya.reddy@demo.local` | `Customer@12345` | Bengaluru |
| Customer | `sanjay.hegde@demo.local` | `Customer@12345` | Shivamogga |
| Customer | `nisha.joshi@demo.local` | `Customer@12345` | Hassan |
| Customer | `imran.khan@demo.local` | `Customer@12345` | Tumakuru |

## Demonstration data

The seed runs only in the `dev` profile, only on an empty database, and only as one all-or-nothing transaction.

- 3 warehouses, 5 categories and 12 products in INR, with stock in every warehouse.
- 10 customers and 31 orders across the last two months, with every shipment status, on-time and late deliveries, cancellations, and one order that no warehouse could fill.

**The history is real output, not inserted rows.** The orders go through the same services as a live order, with the clock moved back, so the allocations, stock ledger and tracking are what the system really produced, and the analytics report them truthfully. Recorded adjustments then return stock to its opening values, so the scenario below starts exactly as specified.

To start again from a clean database, stop the backend, then run `./local-db.sh reset`. It saves a backup first, and the next backend start seeds everything again.

## The 20-step demonstration

Use two browser windows, or one normal and one private, so that both accounts stay logged in.

| Step | Who | Do this | You should see |
|---|---|---|---|
| 1 | Administrator | Log in | The control tower: KPIs, charts, alerts and the network map |
| 2 | Administrator | Open **Warehouses** | Hubli, Bengaluru and Mysuru |
| 3 | Administrator | Open **Inventory**, search `PH-1001` | Smartphones: 15, 50 and 5 available |
| 4 | Customer | Log in as `asha.rao@demo.local` | The shop |
| 5 | Customer | Add 10 **Smartphone** and 5 **Wireless Headphones** | Cart shows 15 items |
| 6 | Customer | Check out to **Davanagere**, Standard, confirm the simulated payment | The order's tracking page |
| 7 to 13 | System | Happens automatically during step 6 | Stock checked everywhere, Mysuru rejected, Hubli and Bengaluru scored, Hubli selected, stock reserved, order confirmed, dispatch created |
| 14 | Administrator | **Orders**, open the new order | Hubli **0.4746** selected, Bengaluru **0.7367**, Mysuru rejected: "needs 10, has 5". Lower scores win. Try the map replay here. |
| 15 | Administrator | **Dispatch**, open the shipment, **Mark as Packed** | Next action becomes Dispatched |
| 16 | Administrator | **Mark as Dispatched** | Reserved stock leaves the warehouse |
| 17 | Customer | Keep the order page open | The timeline updates without reloading, within about 15 seconds |
| 18 | Administrator | **Mark as In Transit** | The map marker moves along its route |
| 19 | Administrator | **Mark as Delivered** | The order is complete |
| 20 | Administrator | Open **Overview** | Total and delivered orders are each one higher |

**Failure path, if time allows.** The seeded order from Priya Nair wants 6 monitors. Each warehouse holds 4, so 12 exist in total, yet every warehouse is rejected because orders are never split. Open that order to see the reasons, restock Hubli by 2 under **Inventory**, then re-run allocation on the order. It is confirmed and gets a dispatch.

Because the demonstration consumes stock, repeat it after `./local-db.sh reset`.

## How a warehouse is chosen

1. **Eligibility is a hard rule.** A warehouse must be active and hold every line of the order in full.
2. **Each eligible warehouse gets a score. Lower is better.**

   `score = 0.40 × distance + 0.30 × delivery cost + 0.20 × inventory factor + 0.10 × priority factor`

   Distance and cost are normalized across the eligible warehouses. Weights are configurable in `backend/src/main/resources/application.yml`, and ties are resolved by fixed rules.
3. **The decision is stored**, including every evaluated warehouse and rejection reason.
4. **Reservation is transaction-safe.** Stock rows are locked before the decision, so two simultaneous orders can't oversell, and the order, decision, reservation and dispatch commit together or not at all.

## Tests

```bash
cd backend  && ./mvnw test                                # real MySQL, in supplychain_test
cd frontend && npm test && npm run lint && npm run build
```

The backend tests include real concurrent orders against MySQL, and run the seed to check that the ledger explains every stock figure.

## Known limits

- Orders are never split across warehouses.
- Deliverable cities are a fixed list of 16 in Karnataka.
- Map positions are approximate, and shipment markers are positioned from status rather than GPS.
- Analytics are computed live from the operational tables, with no caching.
