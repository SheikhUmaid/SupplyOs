# MASTER VIBECODING PROMPT
# AI-POWERED SUPPLY CHAIN OPTIMIZATION & AUTOMATED DISPATCH SYSTEM
## MVP — Professor Demonstration Edition

Act as a Principal Software Architect, Senior Next.js Engineer, Backend Engineer, UI/UX Designer and Software Testing Engineer.

Your responsibility is to design and build a complete, functional, visually impressive MVP of an enterprise-grade supply chain management and automated dispatch platform.

The project is an academic Major Project-1 titled:

**AI-Powered Supply Chain Optimization & Automated Dispatch System**

The application must demonstrate the complete operational workflow of a multi-warehouse supply chain, from customer order placement to warehouse selection, inventory reservation, dispatch generation and delivery tracking.

This is the first MVP intended for demonstration to college professors.

Do not attempt to implement the advanced AI/ML forecasting engine, real-world logistics integrations or unnecessary enterprise infrastructure in this version.

Prioritize:
- Functional completeness.
- Excellent UI/UX.
- Correct business logic.
- Interactive demonstrations.
- Reliable data consistency.
- Clear visual representation of warehouse optimization.
- Clean and maintainable architecture.

The application must feel like an actual enterprise software product rather than a typical college CRUD project.

---

# PHASE 0 — TECHNOLOGY STACK AND ARCHITECTURE

Use the following technologies strictly.

## Frontend

- Next.js (App Router)
- TypeScript
- Tailwind CSS
- Zustand
- Framer Motion
- PixiJS / Canvas 2D
- ESLint
- Prettier
- Vitest

## Visualization Decision

Choose **PixiJS / Canvas 2D** instead of React Three Fiber.

Reason:

This application needs interactive warehouse visualizations, distribution networks, connection lines, delivery routes and operational maps.

A 2D rendering engine is more appropriate than a 3D environment for this particular MVP.

Use PixiJS for:
- Warehouse network visualization.
- Warehouse-to-customer connection lines.
- Shipment movement animations.
- Interactive warehouse nodes.
- Visual warehouse selection.
- Dispatch route visualization.

Do not use PixiJS for ordinary dashboard components, tables, forms or charts.

Use regular React components for those.

## Backend

Follow the backend architecture described in the academic synopsis:

- Java
- Spring Boot
- Spring Security
- Spring Data JPA
- Hibernate
- REST APIs
- JWT authentication

## Database

- MySQL
- Relational database design.
- Proper foreign keys.
- Transactions.
- Database constraints.

## Backend communication

The Next.js frontend must communicate with the Spring Boot backend using REST APIs.

Use a centralized API service layer.

Do not put database access directly inside frontend components.

## Architecture

Use the following architecture:

Customer / Administrator
        |
        v
Next.js Application
        |
        v
REST API Communication
        |
        v
Spring Boot Backend
        |
        v
Business Logic / Services
        |
        v
Spring Data JPA
        |
        v
MySQL Database

PixiJS handles visual simulation and graphical representations in the frontend.

Zustand manages frontend interaction state.

Framer Motion handles UI transitions.

Vitest tests frontend utilities and business decision logic where appropriate.

Do not introduce Redis, Kafka, Docker, Kubernetes or additional infrastructure unless specifically required later.

---

# PHASE 1 — PRODUCT VISION

Build a centralized supply chain management application that allows a company to manage multiple warehouses and fulfil customer orders intelligently.

Imagine a company operating three warehouses:

1. Hubli Warehouse.
2. Bengaluru Warehouse.
3. Mysuru Warehouse.

Each warehouse stores different quantities of products.

When a customer places an order, the application must:

1. Receive the order.
2. Validate the requested products and quantities.
3. Check inventory across all warehouses.
4. Identify eligible warehouses.
5. Compare eligible warehouses.
6. Select the most suitable warehouse.
7. Reserve the required inventory.
8. Confirm the order.
9. Generate a dispatch record.
10. Allow the administrator to update delivery status.
11. Allow the customer to track the order.

The entire process must be demonstrable through the application.

The system should be designed around one central idea:

**The software should make warehouse fulfilment decisions automatically instead of requiring an administrator to manually select a warehouse for every order.**

---

# PHASE 2 — USER ROLES

Implement two user roles.

## 2.1 Administrator

The administrator controls the entire operational system.

Permissions:

- View operational dashboard.
- Manage products.
- Manage categories.
- Manage warehouses.
- Manage inventory.
- View customer information.
- View and manage orders.
- View warehouse allocation decisions.
- Monitor dispatches.
- Update shipment status.
- View inventory alerts.
- View operational analytics.

## 2.2 Customer

Customers interact with the shopping interface.

Permissions:

- Register.
- Login.
- Browse products.
- View product information.
- Add products to cart.
- Place orders.
- View order history.
- Track shipments.
- View their profile.

Implement secure JWT authentication and role-based authorization.

Customers must never be able to access administrator functionality or other customers' orders.

For the demonstration, provide preconfigured development accounts through database seeding.

---

# PHASE 3 — APPLICATION STRUCTURE

Build the following application routes.

## Public and customer routes

- `/`
- `/login`
- `/register`
- `/products`
- `/products/[id]`
- `/cart`
- `/checkout`
- `/orders`
- `/orders/[id]`
- `/profile`

## Administrator routes

- `/admin`
- `/admin/products`
- `/admin/products/new`
- `/admin/warehouses`
- `/admin/inventory`
- `/admin/orders`
- `/admin/orders/[id]`
- `/admin/dispatch`
- `/admin/analytics`

Use protected layouts for administrator routes.

Implement appropriate loading, empty and error states.

---

# PHASE 4 — ADMINISTRATOR CONTROL TOWER

This is the most important interface from a presentation perspective.

Build a modern enterprise control tower.

## Design direction

The visual design should be inspired by professional enterprise logistics and operations software.

Reference design characteristics:

- Modern enterprise SaaS.
- Clean typography.
- Structured layouts.
- White and light-neutral surfaces.
- Deep navy sidebar.
- Blue as the primary accent.
- Subtle borders.
- Professional data tables.
- Clear information hierarchy.
- Consistent spacing.
- Minimal animations.
- Well-designed charts.

Avoid:

- Excessive gradients.
- Overly rounded cards.
- Glassmorphism everywhere.
- Unnecessary decorative graphics.
- Generic AI-generated dashboard designs.
- Excessive empty space.
- Inconsistent typography.

Use a consistent design system.

## 4.1 Sidebar

Create a collapsible sidebar containing:

- Overview
- Products
- Warehouses
- Inventory
- Orders
- Dispatch
- Analytics

At the bottom:
- Administrator profile.
- Settings.
- Logout.

## 4.2 Top navigation

Include:

- Current page title.
- Breadcrumbs.
- Search.
- Notification icon.
- Administrator profile menu.

## 4.3 Overview dashboard

Display the following KPI cards:

- Total Orders.
- Pending Orders.
- Total Warehouses.
- Total Products.
- Total Inventory.
- Active Dispatches.
- Low Stock Items.

Below the KPI cards, include:

### Order Activity
A chart showing order activity over time.

### Warehouse Performance
A comparison of inventory distribution across warehouses.

### Recent Orders
A table showing:

- Order ID.
- Customer.
- Date.
- Amount.
- Allocated Warehouse.
- Status.

### Inventory Alerts
Display products that are below their reorder threshold.

### Recent Dispatches
Show recently generated dispatch records and their status.

All dashboard information must originate from actual backend data.

Never hardcode dashboard statistics.

---

# PHASE 5 — PRODUCT MANAGEMENT

Create a complete product management module.

Product information:

- Product ID.
- Product Name.
- SKU.
- Description.
- Category.
- Price.
- Image.
- Status.
- Created At.

Features:

- Product listing.
- Search.
- Category filters.
- Add product.
- Edit product.
- Delete or deactivate product.
- Product detail view.

Use reusable table and form components.

Include form validation and clear success/error feedback.

---

# PHASE 6 — WAREHOUSE MANAGEMENT

Create a complete warehouse management interface.

Each warehouse must contain:

- Warehouse ID.
- Warehouse Name.
- City.
- State.
- Address.
- Latitude.
- Longitude.
- Contact Number.
- Estimated Delivery Cost Parameters.
- Warehouse Status.

Features:

- Create warehouse.
- Edit warehouse.
- View warehouse details.
- Activate/deactivate warehouse.
- View associated inventory.
- View warehouse performance.

Seed the application with these demonstration warehouses:

1. Hubli.
2. Bengaluru.
3. Mysuru.

Use approximate geographic coordinates for visualization.

Each warehouse must have a corresponding inventory dataset.

---

# PHASE 7 — INVENTORY MANAGEMENT

Implement inventory management across multiple warehouses.

The inventory system connects:

- Products.
- Warehouses.
- Available quantities.
- Reserved quantities.

Each inventory record must contain:

- Product.
- Warehouse.
- Available Quantity.
- Reserved Quantity.
- Reorder Level.
- Last Updated.

Features:

- View inventory across warehouses.
- Filter by warehouse.
- Search by product.
- Add stock.
- Update stock.
- View low-stock items.
- View inventory movement history.

Display stock information using:

- Quantity labels.
- Progress indicators.
- Status badges.
- Low-stock warnings.

Important business rules:

- Stock must never become negative.
- Reserved inventory must not be available for new orders.
- Every stock modification must be recorded.
- Inventory updates must be transaction-safe.

Use database transactions and appropriate locking.

---

# PHASE 8 — CUSTOMER SHOPPING EXPERIENCE

Build a functional customer-facing shopping interface.

## Home page

Include:

- Company branding.
- Navigation.
- Product catalogue.
- Featured products.
- Categories.
- Search.
- Cart indicator.

## Product details

Display:

- Product image.
- Product name.
- SKU.
- Description.
- Price.
- Availability.
- Quantity selector.
- Add to cart.

## Shopping cart

Display:

- Selected products.
- Quantities.
- Unit prices.
- Subtotals.
- Total amount.

Allow customers to update quantities or remove products.

## Checkout

Collect:

- Customer name.
- Contact number.
- Delivery address.
- City.
- State.
- Postal code.

Display the order summary.

Use a simulated payment confirmation for the academic demonstration.

Do not integrate an actual payment gateway.

After successful checkout, create the order through the backend.

---

# PHASE 9 — AUTOMATED WAREHOUSE SELECTION ENGINE

This is the most important business logic component.

Implement a warehouse-selection engine.

It must execute automatically whenever a valid order is submitted.

## Example

A customer places an order for:

- 10 smartphones.
- 5 wireless headphones.

Available inventory:

| Warehouse | Smartphones | Headphones |
|---|---:|---:|
| Hubli | 15 | 10 |
| Bengaluru | 50 | 30 |
| Mysuru | 5 | 20 |

The system must first reject Mysuru because it cannot fulfil the complete order.

Hubli and Bengaluru remain eligible.

The system then compares eligible warehouses.

## Selection parameters

Consider:

1. Distance from warehouse to customer.
2. Estimated delivery cost.
3. Inventory suitability.
4. Order priority.

Implement a normalized weighted scoring algorithm.

Conceptually:

Score = W1(Distance) + W2(Delivery Cost) + W3(Inventory Factor) + W4(Priority Factor)

The scoring implementation must clearly define:

- How each parameter is normalized.
- Whether lower or higher scores are preferred.
- What each weight represents.
- How ties are resolved.

Use sensible default weights and make them configurable.

Stock availability is a hard constraint.

A warehouse that cannot fulfil the complete order must never be selected.

## Explainability

This is essential for the professor demonstration.

When an order is allocated, display:

- All evaluated warehouses.
- Eligible warehouses.
- Rejected warehouses.
- Reasons for rejection.
- Distance comparison.
- Estimated delivery cost.
- Final score.
- Selected warehouse.
- Explanation of why it was selected.

Example:

"Hubli Warehouse selected because it has sufficient inventory and offers the lowest estimated delivery cost among eligible warehouses."

The selection decision must be stored in the database.

Start with a simple rule-based algorithm and then implement weighted scoring.

Do not use an LLM to select warehouses.

The algorithm must be deterministic and testable.

---

# PHASE 10 — INVENTORY RESERVATION

Implement reliable inventory reservation.

Example:

A warehouse has 10 smartphones.

Two customers simultaneously place orders for 7 smartphones each.

Only one order should successfully reserve 7 units.

The second order must fail or be re-evaluated against other eligible warehouses.

Requirements:

- Database transactions.
- Atomic stock reservation.
- Appropriate row locking or optimistic concurrency handling.
- Rollback on failure.
- Reservation history.
- Prevention of negative inventory.
- Correct handling of cancelled orders.

Do not confirm orders until inventory has been successfully reserved.

Make this functionality a priority over visual polish.

---

# PHASE 11 — AUTOMATED DISPATCH

Once an order has been successfully allocated and confirmed, automatically generate a dispatch record.

Dispatch fields:

- Dispatch ID.
- Order ID.
- Warehouse ID.
- Dispatch Date.
- Shipment Status.
- Estimated Delivery Date.
- Tracking Reference.
- Delivery Date.

Shipment lifecycle:

1. Pending.
2. Packed.
3. Dispatched.
4. In Transit.
5. Delivered.

Create a dispatch management page.

Display:

- Dispatch ID.
- Associated Order.
- Customer.
- Warehouse.
- Dispatch Date.
- Estimated Delivery.
- Current Status.

Allow the administrator to update the shipment status.

Every status change must be recorded in a tracking history.

Implement valid status transitions.

For example, a delivered shipment should not be moved back to pending.

---

# PHASE 12 — INTERACTIVE SUPPLY CHAIN VISUALIZATION

This is the main visual differentiator of the project.

Use PixiJS / Canvas 2D.

Build an interactive supply-chain network visualization.

## Visualization design

Display a simplified geographic map of the demonstration region.

Show:

- Hubli Warehouse.
- Bengaluru Warehouse.
- Mysuru Warehouse.
- Customer delivery locations.
- Warehouse connection lines.
- Shipment routes.

Each warehouse should appear as a distinct interactive node.

## Interactions

When an administrator selects a warehouse:

- Highlight the selected warehouse.
- Show warehouse information.
- Display inventory summary.
- Display active dispatch count.

When an order is allocated:

- Highlight the selected warehouse.
- Draw a route connecting the selected warehouse to the customer.
- Display the order identifier.
- Show the selected warehouse's name.

When a dispatch is updated:

- Reflect the corresponding shipment state visually.

## Animation

Use Framer Motion for UI transitions.

Use PixiJS for canvas animations.

Avoid unnecessary animation.

Prioritize clarity and performance.

The visualization should communicate actual application data rather than displaying decorative animations unrelated to backend state.

---

# PHASE 13 — ORDER TRACKING

Create a professional order-tracking interface.

Customers should be able to see their shipment progress.

Display a timeline:

- Order Placed.
- Order Confirmed.
- Warehouse Allocated.
- Packed.
- Dispatched.
- In Transit.
- Delivered.

Display:

- Order ID.
- Product details.
- Selected warehouse.
- Delivery address.
- Estimated delivery date.
- Current shipment status.
- Status history.

The administrator should be able to see the same information with additional operational details.

Do not implement real GPS tracking.

All status changes should come from actual backend records.

---

# PHASE 14 — ANALYTICS

Build an analytics dashboard using Recharts.

Include:

- Order volume over time.
- Warehouse-wise order distribution.
- Inventory distribution.
- Top-selling products.
- Dispatch status distribution.
- Low-stock products.
- Order processing statistics.

Include filters where useful.

Use actual backend data.

Do not fabricate analytics.

Do not implement ML-based demand forecasting in this MVP.

A rule-based low-stock report is sufficient for the first demonstration.

---

# PHASE 15 — ZUSTAND STATE MANAGEMENT

Use Zustand for client-side application state.

Create separate stores where appropriate:

1. Auth Store.
2. Cart Store.
3. UI Store.
4. Dashboard Filter Store.
5. Warehouse Visualization Store.

Manage:

- Logged-in user information.
- Cart contents.
- Sidebar state.
- Active filters.
- Selected warehouse.
- Selected order.
- Selected dispatch.
- Visualization interactions.

Do not duplicate backend database state unnecessarily in Zustand.

Server-owned data must be fetched from backend APIs.

Persist only appropriate client-side state.

Never persist sensitive authentication information insecurely.

---

# PHASE 16 — FRAMER MOTION

Use Framer Motion for:

- Sidebar transitions.
- Modal animations.
- Drawer transitions.
- Page transitions where appropriate.
- Dropdown animations.
- Toast notifications.
- Order status transitions.
- Loading and feedback states.

Keep animations subtle and professional.

Respect reduced-motion accessibility preferences.

---

# PHASE 17 — BACKEND INTEGRATION

Use the following backend modules:

- auth
- users
- products
- categories
- warehouses
- inventory
- orders
- allocation
- dispatch
- tracking
- analytics

Use a layered architecture:

Controller → Service → Repository → Database.

Implement:

- DTOs.
- Entity relationships.
- Validation.
- Exception handling.
- JWT authentication.
- Role-based authorization.
- Transaction management.
- Pagination.
- Search and filtering.

Use Swagger/OpenAPI documentation.

Keep frontend API contracts consistent with backend implementations.

---

# PHASE 18 — DATABASE DESIGN

Create a normalized relational database.

Required tables:

1. users
2. categories
3. products
4. warehouses
5. inventory
6. inventory_transactions
7. orders
8. order_items
9. warehouse_allocations
10. dispatches
11. delivery_tracking

Design relationships carefully.

For example:

One warehouse has many inventory records.

One product can exist in multiple warehouses.

One order contains multiple order items.

One order has an allocation record.

One order can have a dispatch record.

One dispatch can have multiple tracking updates.

Use appropriate constraints and indexes.

Prepare an ER diagram before implementing entities.

---

# PHASE 19 — DEMONSTRATION DATA

Seed the database with realistic sample data.

Create:

- 3 warehouses.
- 10–15 products.
- 10 customer accounts.
- Multiple inventory records.
- At least 15 historical orders.
- Multiple dispatch records.
- Different delivery statuses.
- Several low-stock products.

Use realistic INR pricing.

Create meaningful sample scenarios.

At least one scenario must demonstrate:

- A warehouse being rejected due to insufficient inventory.
- Multiple eligible warehouses.
- Automatic selection of the best warehouse.
- Inventory reservation.
- Automatic dispatch creation.

Provide development login credentials in the README only.

Never hardcode secrets in production source code.

---

# PHASE 20 — TESTING

Use Vitest for frontend and decision-engine unit tests.

Test:

- Warehouse eligibility.
- Insufficient stock rejection.
- Score normalization.
- Warehouse ranking.
- Tie-breaking.
- Cart calculations.
- Inventory validation utilities.
- Shipment status transitions.

Backend tests should cover:

- Authentication.
- Role permissions.
- Inventory transactions.
- Concurrent reservations.
- Order creation.
- Warehouse allocation.
- Dispatch creation.
- Shipment updates.

Do not mark features complete without executing the relevant tests.

---

# PHASE 21 — MVP EXCLUSIONS

Do not implement the following in this version:

- Actual AI/ML demand forecasting.
- Python machine learning service.
- LLM integration.
- Real GPS tracking.
- Live traffic integration.
- External courier APIs.
- Actual payment gateway.
- Physical warehouse automation.
- IoT sensors.
- Redis.
- Kafka.
- Kubernetes.
- Complex microservice architecture.
- Advanced route optimization.

These are future enhancements.

Do not create fake AI predictions or pretend that rule-based functionality is machine learning.

The objective is to complete the core operational workflow first.

---

# PHASE 22 — DEVELOPMENT EXECUTION RULES

Follow an incremental development approach.

Do not generate the entire application in one response.

## Phase 0 — Architecture and planning

First provide:

- System architecture.
- Complete folder structure.
- Database ER diagram.
- Entity relationships.
- API endpoint plan.
- User journey.
- Administrator journey.
- Warehouse allocation workflow.
- PixiJS visualization plan.

Wait for approval.

## Phase 1 — Foundation

Set up:

- Next.js.
- TypeScript.
- Tailwind CSS.
- Zustand.
- Framer Motion.
- PixiJS.
- ESLint.
- Prettier.
- Vitest.

Establish the design system.

## Phase 2 — Backend and database

Create the Spring Boot application.

Configure MySQL.

Implement entities, repositories, authentication and authorization.

Test all initial APIs.

## Phase 3 — Management modules

Implement:

- Products.
- Categories.
- Warehouses.
- Inventory.

## Phase 4 — Customer workflow

Implement:

- Customer registration.
- Login.
- Product browsing.
- Cart.
- Checkout.
- Order placement.

## Phase 5 — Supply chain engine

Implement:

- Inventory verification.
- Warehouse eligibility.
- Weighted scoring.
- Warehouse allocation.
- Inventory reservation.

Write tests before proceeding.

## Phase 6 — Dispatch and tracking

Implement automatic dispatch creation and shipment lifecycle management.

## Phase 7 — Visualization

Implement the PixiJS supply-chain network visualization.

Connect it to actual warehouse, order and dispatch data.

## Phase 8 — Analytics and dashboard

Complete the administrator dashboard and analytics.

## Phase 9 — Integration and testing

Test the entire application.

Fix errors.

Verify database consistency.

Verify all user workflows.

Prepare demonstration data.

Prepare the README.

---

# PHASE 23 — STRICT CODING RULES

1. Never generate the entire application at once.
2. Never skip architecture planning.
3. Never create unnecessary components.
4. Never duplicate business logic.
5. Never use static JSON to simulate completed backend functionality.
6. Never hardcode dashboard statistics.
7. Never expose sensitive information.
8. Never bypass authentication.
9. Never allow users to access other users' orders.
10. Never allow negative inventory.
11. Never confirm orders without successful inventory reservation.
12. Never generate dispatches for failed orders.
13. Never make warehouse selection randomly.
14. Never use decorative visualizations that do not reflect actual data.
15. Never claim AI/ML functionality has been implemented when it has not.
16. Keep TypeScript types strict.
17. Use reusable components.
18. Keep API communication centralized.
19. Follow ESLint and Prettier conventions.
20. Write tests for important business logic.
21. Keep frontend and backend API contracts synchronized.
22. Do not silently change the agreed architecture.
23. Document complicated decisions.
24. Maintain a development progress checklist.

If an issue is discovered, explain the cause, implement a proper fix and run the relevant tests.

Do not hide errors or work around them with fake data.

---

# PHASE 24 — FINAL DEMONSTRATION REQUIREMENTS

The final MVP must support the following demonstration scenario:

1. Administrator logs into the control tower.
2. Administrator views three warehouses.
3. Administrator opens inventory and examines available stock.
4. Customer logs into the shopping interface.
5. Customer selects multiple products.
6. Customer places an order.
7. System checks stock across all three warehouses.
8. System identifies eligible warehouses.
9. System compares distance, delivery cost and inventory suitability.
10. System selects the most suitable warehouse.
11. System reserves inventory.
12. System confirms the order.
13. System creates a dispatch record.
14. Administrator views the allocation decision.
15. Administrator updates the dispatch to Packed.
16. Administrator updates it to Dispatched.
17. Customer sees the tracking status change.
18. Administrator updates it to In Transit.
19. Administrator marks it Delivered.
20. Dashboard reflects the updated operational data.

The entire workflow must function end to end.

---

# FINAL INSTRUCTION

Start with Phase 0 only.

Do not write application code yet.

Analyse the requirements and produce a complete architecture proposal, database design, ER diagram, API specification, frontend structure and implementation roadmap.

Explain any technical trade-offs.

Identify missing requirements and potential risks.

Wait for explicit approval before beginning implementation.

Your final objective is to deliver a polished, reliable and fully functional academic MVP that demonstrates the core supply-chain optimization and automated dispatch workflow through a professional enterprise interface.

**Build a real working product, not a collection of attractive screens.**