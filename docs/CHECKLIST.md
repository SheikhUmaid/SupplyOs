# Development Checklist

Mark an item complete only after its relevant tests have been run. The design is in [ARCHITECTURE.md](ARCHITECTURE.md).

## Phase 0: Architecture
- [x] Architecture, ER diagram, API specification, frontend and backend structure, roadmap
- [x] **Approved by project owner** (2026-11-01)

## Phase 1: Frontend foundation
- [x] Next.js, TypeScript (strict), Tailwind, Zustand, Framer Motion, PixiJS, Recharts
- [x] ESLint, Prettier, Vitest
- [x] Design system and shared UI components
- [x] Customer and admin layouts, collapsible sidebar, top navigation
- [x] Central API client
- [x] Lint, type check, build and tests pass (67 tests; layouts and components also checked in Chrome)
- [x] **Approved by project owner** (2026-11-01)

## Phase 2: Backend foundation
- [x] MySQL database and user created (private instance on port 3307 via `backend/local-db.sh`, since the system MySQL needs root access)
- [x] Spring Boot project with Maven Wrapper, Java 21 target, port 8081
- [x] Flyway schema: 12 tables, FKs, CHECKs, indexes
- [x] Entities and repositories
- [x] JWT in HttpOnly cookie, role-based authorization
- [x] Login page, `/admin` route guard, real logout and current user in the layouts (carried over from Phase 1, which had no backend)
- [x] Global error handling, Swagger
- [x] Auth and authorization tests pass on MySQL (so far on a private temporary MySQL 8.0.46, not the owner's database)
- [x] Tests rerun on the project's `supplychain_test` database (173 passing, 2026-11-01)
- [x] **Approved by project owner** (2026-11-01; database rerun still open)

## Phase 3: Management modules
- [x] Categories and products, with deactivation
- [x] Warehouses, with deactivation and performance view
- [x] Inventory, stock changes, movement ledger, low-stock view
- [x] Admin screens connected to the real API
- [x] Tests pass (on the private temporary MySQL; see Phase 2)
- [x] **Approved by project owner** (2026-11-01)

## Phase 4: Customer ordering
- [x] Registration, login, profile
- [x] Home, catalogue, product details
- [x] Cart with persistence
- [x] Checkout with city list, priority and simulated payment
- [x] Order placement, order history (orders stay `PENDING` until Phase 5 adds allocation)
- [x] Demonstration warehouses, categories, products and stock (moved up from Phase 9)
- [x] Tests pass (on the private temporary MySQL; see Phase 2)
- [x] **Approved by project owner** (2026-11-01)

## Phase 5: Optimization engine
- [x] Eligibility gate with specific rejection reasons
- [x] Weighted scoring, normalization, tie-breaks
- [x] Stored, explainable allocations
- [x] Locked, transactional reservation with rollback
- [x] Pending flow and re-run allocation
- [x] Cancellation and stock release
- [x] Allocation comparison screen, admin order list, settings page
- [x] Engine tests pass
- [x] Concurrency tests pass on real MySQL (the private temporary instance; see Phase 2)
- [ ] **Approved by project owner**

## Phase 6: Dispatch and tracking
- [ ] Automatic dispatch creation, inside the order's transaction
- [ ] Cancelling an order also cancels its dispatch
- [ ] Status transitions, with invalid moves rejected
- [ ] Stock leaves reserved on dispatch
- [ ] Dispatch board
- [ ] Customer timeline from real records
- [ ] Warehouse drawer lists allocated orders and dispatch history (their APIs arrive in Phases 5 and 6)
- [ ] Tests pass

## Phase 7: Visualization
- [ ] Map scene, projection, warehouse nodes, customer points
- [ ] Routes and links from real data
- [ ] Allocation replay from stored evaluations
- [ ] Shipment markers driven by status, labelled as simulated
- [ ] Reduced motion and cleanup verified

## Phase 8: Dashboard and analytics
- [ ] 8 KPI cards and dashboard sections
- [ ] Analytics pages using Recharts
- [ ] Figures checked against direct SQL

## Phase 9: Integration
- [ ] Seed data: historical orders, 10 customer accounts, every shipment status (catalogue done in Phase 4)
- [ ] 20-step demo from `Tasks.md` runs from a clean database
- [ ] Database consistency verified (stock totals match the ledger)
- [ ] All tests pass
- [ ] Final README with setup and demo credentials
