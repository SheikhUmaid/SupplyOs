# PROJECT: SUPPLYCHAIN OS
## AI-Powered Supply Chain Optimization & Automated Dispatch System
### MVP — Interactive Enterprise Supply Chain Management Platform

---

# 0. PURPOSE OF THIS DOCUMENT

This document is the complete standalone engineering specification for the first MVP of an application titled **AI-Powered Supply Chain Optimization & Automated Dispatch System**.

It contains the functional requirements, architecture, technology stack, user workflows, data models, interface specifications, business logic, visualization requirements and implementation guidelines.

An AI coding agent or software engineer should be able to develop the application using this document without requiring additional project specifications.

The objective is to build a functional, interactive and visually impressive academic project demonstration.

**Important:** This is an MVP. The advanced AI/ML demand forecasting engine is intentionally excluded. The focus is on demonstrating the operational supply chain workflow and automated warehouse allocation.

---

# 1. WHAT THIS PROJECT IS

SupplyChain OS is a centralized web application that demonstrates how a business can manage multiple warehouses, maintain inventory, process customer orders and automatically determine the most suitable warehouse for fulfilling each order.

Imagine a company operating three warehouses:

1. Hubli
2. Bengaluru
3. Mysuru

Each warehouse stores different quantities of products.

When a customer places an order, the application evaluates the available inventory across all warehouses, identifies eligible warehouses and selects the most suitable warehouse using a weighted decision-making algorithm.

After allocation, the system reserves inventory, confirms the order and generates a dispatch record.

Administrators can monitor the entire operation through a centralized control tower.

Customers can browse products, place orders and track deliveries.

## Core concept

The system automates the following workflow:

Customer Order → Inventory Verification → Warehouse Eligibility → Warehouse Optimization → Inventory Reservation → Order Confirmation → Dispatch Generation → Delivery Tracking.

The application must demonstrate this complete workflow through an interactive web interface.

## What makes this project different?

Traditional inventory management systems primarily record stock and orders.

This project additionally demonstrates automated decision-making.

Instead of asking an administrator to manually choose a warehouse for every order, the system evaluates available options and selects the most suitable one.

The decision must be explainable and visible to the administrator.

---

# 2. PROJECT OBJECTIVES

The MVP must achieve the following objectives:

1. Centralized management of multiple warehouses.
2. Real-time inventory visibility across warehouses.
3. Customer-facing product catalogue and ordering.
4. Automated order processing.
5. Automatic warehouse eligibility checking.
6. Weighted warehouse selection.
7. Reliable inventory reservation.
8. Automatic dispatch record generation.
9. Shipment status management.
10. Customer-facing delivery tracking.
11. Operational analytics and reporting.
12. Interactive geographical warehouse visualization.

The project should demonstrate the practical application of supply chain management, inventory optimization and automated business decision-making.

---

# 3. TECHNOLOGY STACK

Use the following technologies.

## 3.1 Frontend

| Technology | Purpose |
|---|---|
| Next.js | Application framework |
| TypeScript | Type safety |
| Tailwind CSS | Styling |
| Zustand | Client-side state management |
| Framer Motion | UI animations and transitions |
| PixiJS / Canvas 2D | Interactive supply chain visualization |
| ESLint | Code quality |
| Prettier | Code formatting |
| Vitest | Unit testing |

### Visualization decision

Use PixiJS / Canvas 2D.

Do not use React Three Fiber or Three.js.

The application requires a two-dimensional representation of warehouses, customers, routes and shipments. PixiJS provides a suitable rendering approach without unnecessary 3D complexity.

## 3.2 Backend

| Technology | Purpose |
|---|---|
| Java | Backend programming language |
| Spring Boot | Backend framework |
| Spring Security | Authentication and authorization |
| Spring Data JPA | Database interaction |
| Hibernate | ORM |
| JWT | Authentication tokens |
| REST APIs | Frontend/backend communication |

## 3.3 Database

- MySQL
- Relational database architecture.
- Foreign-key constraints.
- Database transactions.
- Appropriate indexes.
- Data validation.

## 3.4 Development tools

- Git.
- GitHub.
- Postman.
- Swagger/OpenAPI.
- IntelliJ IDEA or VS Code.

## 3.5 Architecture restrictions

Do not introduce unnecessary infrastructure.

The MVP does not require:

- Redis.
- Kafka.
- Kubernetes.
- Microservices.
- Docker.
- External payment gateways.
- External courier integrations.

Use a modular monolithic Spring Boot backend.

The Python machine learning service is not part of this MVP.

---

# 4. CORE USER FLOW

The application contains two main user experiences.

1. Administrator Control Tower.
2. Customer Shopping Interface.

## 4.1 Administrator workflow

1. Administrator logs into the system.
2. Administrator opens the control tower.
3. Administrator views warehouse and inventory information.
4. Administrator manages products and warehouses.
5. Administrator monitors incoming customer orders.
6. Administrator opens an order and views its allocation decision.
7. System automatically evaluates warehouses when an order is placed.
8. System selects the most suitable warehouse.
9. System reserves inventory.
10. System confirms the order.
11. System creates a dispatch record.
12. Administrator monitors shipment progress.
13. Administrator updates delivery status.
14. Administrator views updated analytics.

## 4.2 Customer workflow

1. Customer opens the shopping interface.
2. Customer registers or logs in.
3. Customer browses available products.
4. Customer views product details.
5. Customer adds products to the cart.
6. Customer proceeds to checkout.
7. Customer enters delivery information.
8. Customer confirms the order.
9. System processes warehouse allocation.
10. Customer receives order confirmation.
11. Customer views order details.
12. Customer tracks the delivery.

No actual payment gateway is required for this MVP.

Use a simulated payment confirmation.

---

# 5. SCREENS AND APPLICATION ROUTES

Unlike a single-screen simulation, this application requires multiple operational pages.

Do not create unnecessary pages outside the following scope.

## 5.1 Public and customer pages

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

## 5.2 Administrator pages

- `/admin`
- `/admin/products`
- `/admin/products/new`
- `/admin/warehouses`
- `/admin/inventory`
- `/admin/orders`
- `/admin/orders/[id]`
- `/admin/dispatch`
- `/admin/analytics`

The administrator interface must use a common dashboard layout.

The customer interface must use a separate shopping layout.

---

# 6. ADMINISTRATOR CONTROL TOWER

The Control Tower is the central operational interface.

It must provide an overview of the company's entire supply chain.

## 6.1 Visual design

Use an enterprise-grade design.

Design characteristics:

- Light neutral background.
- Deep navy sidebar.
- White content surfaces.
- Blue primary accent.
- Professional typography.
- Subtle borders.
- Consistent spacing.
- Clean data tables.
- Clear status indicators.
- Responsive layouts.

Avoid excessive gradients, glassmorphism, oversized rounded cards and unnecessary decorative elements.

The interface should resemble professional enterprise logistics software.

## 6.2 Sidebar navigation

Include:

- Overview.
- Products.
- Warehouses.
- Inventory.
- Orders.
- Dispatch.
- Analytics.

At the bottom:

- Administrator profile.
- Settings.
- Logout.

The sidebar should support collapse and expand interactions.

## 6.3 Top navigation

Include:

- Page title.
- Breadcrumbs.
- Search.
- Notifications.
- Administrator profile menu.

## 6.4 Dashboard overview

Display the following statistics:

- Total Orders.
- Pending Orders.
- Total Products.
- Total Warehouses.
- Total Inventory.
- Active Dispatches.
- Delivered Orders.
- Low Stock Items.

Include the following dashboard sections.

### Order activity

Display order volume over time using a line or area chart.

### Warehouse performance

Compare warehouses using:

- Inventory quantity.
- Allocated orders.
- Completed dispatches.
- Fulfilment performance.

### Recent orders

Display:

- Order ID.
- Customer.
- Order Date.
- Amount.
- Selected Warehouse.
- Status.

### Inventory alerts

Display products below their reorder levels.

### Recent dispatches

Display:

- Dispatch ID.
- Warehouse.
- Order ID.
- Shipment Status.

All dashboard statistics must come from actual application data.

---

# 7. PRODUCT MANAGEMENT

Administrators must be able to manage the company's product catalogue.

## Product data model

Each product must contain:

- Product ID.
- Product Name.
- SKU.
- Description.
- Category.
- Price.
- Image URL.
- Status.
- Created At.
- Updated At.

## Features

- Create products.
- View products.
- Edit products.
- Deactivate products.
- Search products.
- Filter by category.
- View individual product details.

SKU must be unique.

Products must not be physically deleted when they are associated with historical orders. Use deactivation instead.

---

# 8. WAREHOUSE MANAGEMENT

Administrators must be able to manage multiple warehouses.

## Warehouse data model

Each warehouse must contain:

- Warehouse ID.
- Warehouse Name.
- Address.
- City.
- State.
- Postal Code.
- Latitude.
- Longitude.
- Contact Number.
- Estimated Delivery Cost Parameters.
- Operational Status.
- Created At.

## Initial demonstration warehouses

Use the following locations:

1. Hubli, Karnataka.
2. Bengaluru, Karnataka.
3. Mysuru, Karnataka.

Use approximate geographic coordinates.

## Features

- Create warehouse.
- Edit warehouse.
- View warehouse details.
- Activate/deactivate warehouse.
- View associated inventory.
- View allocated orders.
- View dispatch history.
- View warehouse performance.

Inactive warehouses must not be considered for new order allocations.

---

# 9. INVENTORY MANAGEMENT

Inventory management is a central module.

The system must maintain stock independently for every product in every warehouse.

For example:

| Product | Hubli | Bengaluru | Mysuru |
|---|---:|---:|---:|
| Smartphone | 15 | 50 | 5 |
| Laptop | 10 | 20 | 12 |
| Headphones | 30 | 40 | 25 |
| Keyboard | 12 | 18 | 30 |

These are demonstration values.

## Inventory data model

Each inventory record contains:

- Inventory ID.
- Product ID.
- Warehouse ID.
- Available Quantity.
- Reserved Quantity.
- Reorder Level.
- Last Updated.

## Features

- View all inventory.
- Filter by warehouse.
- Search by product.
- Add stock.
- Update stock.
- View low-stock items.
- View stock movement history.
- View warehouse-specific inventory.

## Inventory rules

- Available quantity cannot become negative.
- Reserved quantity cannot exceed physical stock.
- All stock modifications must be recorded.
- Stock must be updated transactionally.
- Historical inventory transactions must remain accessible.

---

# 10. CUSTOMER SHOPPING INTERFACE

Create a professional customer-facing shopping experience.

## 10.1 Home page

Include:

- Brand logo.
- Navigation.
- Product search.
- Product categories.
- Featured products.
- Shopping cart indicator.
- Customer account menu.

## 10.2 Product catalogue

Display products in a responsive grid.

Each product card should contain:

- Product image.
- Product name.
- Category.
- Price in INR.
- Availability.
- View Details action.
- Add to Cart action.

## 10.3 Product details

Display:

- Product image.
- Product name.
- SKU.
- Description.
- Price.
- Quantity selector.
- Add to Cart button.

## 10.4 Shopping cart

Display:

- Selected products.
- Quantity.
- Unit price.
- Subtotal.
- Total amount.

Allow customers to:

- Increase quantity.
- Decrease quantity.
- Remove products.
- Continue shopping.
- Proceed to checkout.

## 10.5 Checkout

Collect:

- Customer Name.
- Contact Number.
- Address.
- City.
- State.
- Postal Code.

Display an order summary.

Provide a simulated payment confirmation for the demonstration.

Do not integrate Razorpay or other real payment gateways.

---

# 11. ORDER MANAGEMENT

The order management module handles all incoming customer orders.

## Order data model

An order should contain:

- Order ID.
- Customer ID.
- Order Date.
- Delivery Address.
- Total Amount.
- Order Status.
- Selected Warehouse.
- Payment Status.

## Order items

Each order item contains:

- Order Item ID.
- Order ID.
- Product ID.
- Quantity.
- Unit Price.
- Subtotal.

## Order statuses

- Pending.
- Confirmed.
- Processing.
- Dispatched.
- Delivered.
- Cancelled.

## Features

- Create orders.
- View all orders.
- View order details.
- Search orders.
- Filter by status.
- View selected warehouse.
- View allocation details.
- View order history.

An order must not be confirmed until inventory has been successfully reserved.

---

# 12. AUTOMATED WAREHOUSE SELECTION

This is the central decision-making component of the project.

The system must automatically select the most suitable warehouse for fulfilling an order.

No manual warehouse selection should be required during the normal customer ordering workflow.

## 12.1 Example scenario

A customer places an order for:

- 10 smartphones.
- 5 wireless headphones.

Available inventory:

| Warehouse | Smartphones | Headphones |
|---|---:|---:|
| Hubli | 15 | 10 |
| Bengaluru | 50 | 30 |
| Mysuru | 5 | 20 |

First, the system checks inventory availability.

Mysuru must be rejected because it does not have enough smartphones.

Hubli and Bengaluru remain eligible.

The system then evaluates both warehouses.

## 12.2 Decision factors

Consider:

1. Distance from warehouse to customer.
2. Estimated delivery cost.
3. Inventory suitability.
4. Order priority.

## 12.3 Weighted scoring algorithm

Implement a normalized weighted scoring algorithm.

Conceptually:

`Score = W1 × Distance + W2 × DeliveryCost + W3 × InventoryFactor + W4 × PriorityFactor`

Each factor must be normalized before calculating the score.

Define clearly:

- What each factor represents.
- How the normalization works.
- What each weight represents.
- Whether lower or higher scores are preferred.
- How ties are resolved.

For example, if lower scores represent better choices:

- Shorter distance should reduce the score.
- Lower delivery cost should reduce the score.
- Better inventory suitability should reduce the score.
- Higher priority should improve the selection score.

Inventory availability is a mandatory eligibility condition, not merely a scoring factor.

A warehouse that cannot fulfil the entire order must never be selected.

## 12.4 Explainable allocation

The system must retain the decision details.

For every allocation, store:

- Order ID.
- Evaluated warehouses.
- Eligible warehouses.
- Rejected warehouses.
- Rejection reasons.
- Distance values.
- Estimated delivery costs.
- Individual scores.
- Final scores.
- Selected warehouse.
- Allocation timestamp.

The administrator must be able to see why the system selected a particular warehouse.

Example explanation:

"Hubli Warehouse was selected because it had sufficient inventory and the lowest weighted fulfilment score among eligible warehouses."

## 12.5 Allocation visualization

When the administrator opens an order's allocation details, show a comparison.

| Warehouse | Eligibility | Score | Decision |
|---|---|---:|---|
| Hubli | Eligible | 0.18 | Selected |
| Bengaluru | Eligible | 0.72 | Not selected |
| Mysuru | Insufficient stock | — | Rejected |

Use illustrative values for demonstration; actual values must be calculated from the configured data.

---

# 13. INVENTORY RESERVATION AND TRANSACTION SAFETY

This is a mandatory backend requirement.

Consider:

A warehouse has 10 smartphones.

Two customers simultaneously order 7 smartphones each.

The system must never allocate 14 smartphones from stock of 10.

Implement:

- Database transactions.
- Atomic stock reservation.
- Appropriate database row locking or optimistic locking.
- Availability verification.
- Rollback on failures.
- Inventory transaction history.

## Required workflow

1. Receive order request.
2. Start database transaction.
3. Verify product availability.
4. Identify eligible warehouses.
5. Select the best warehouse.
6. Lock the required inventory records.
7. Verify stock again.
8. Reserve the requested quantities.
9. Save allocation details.
10. Create order confirmation.
11. Create dispatch record.
12. Commit transaction.

If any critical step fails, roll back the transaction.

The system must never confirm an order with an unsuccessful reservation.

Cancelled orders must release reserved inventory when appropriate.

---

# 14. AUTOMATED DISPATCH MANAGEMENT

After successful warehouse allocation and inventory reservation, the system must automatically generate a dispatch record.

## Dispatch data model

- Dispatch ID.
- Order ID.
- Warehouse ID.
- Dispatch Date.
- Estimated Delivery Date.
- Tracking Reference.
- Shipment Status.
- Delivery Date.

## Shipment statuses

1. Pending.
2. Packed.
3. Dispatched.
4. In Transit.
5. Delivered.

## Administrator features

- View dispatches.
- Search dispatches.
- Filter by shipment status.
- View dispatch details.
- Update shipment status.
- View shipment history.

Every status update must be recorded.

Only valid status transitions should be permitted.

A delivered shipment cannot be moved backwards to pending.

For the MVP, status changes are manual.

No actual courier service or GPS integration is required.

---

# 15. INTERACTIVE SUPPLY CHAIN MAP

This is the primary visual differentiator of the application.

Use PixiJS / Canvas 2D to build an interactive supply-chain visualization.

## 15.1 Map design

Display a simplified geographic representation of Karnataka.

Include:

- Hubli Warehouse.
- Bengaluru Warehouse.
- Mysuru Warehouse.
- Customer delivery locations.
- Warehouse connection lines.
- Shipment routes.

Use approximate geographical coordinates.

The visualization does not need to be a geographically precise navigation system.

## 15.2 Warehouse nodes

Each warehouse should appear as an interactive node.

When selected, display:

- Warehouse name.
- Location.
- Inventory summary.
- Available products.
- Active orders.
- Active dispatches.

## 15.3 Order allocation animation

When an order is allocated:

1. Highlight all evaluated warehouses.
2. Indicate warehouses that lack sufficient stock.
3. Highlight eligible warehouses.
4. Highlight the selected warehouse.
5. Draw a connection between the selected warehouse and the customer.
6. Display the order reference.

## 15.4 Dispatch animation

When a shipment is dispatched:

- Display a shipment marker.
- Animate its movement along the selected route.
- Reflect the current shipment status.
- Allow the administrator to inspect the shipment.

The animation should represent the simulated dispatch process, not claim to show real GPS position.

## 15.5 Performance

Keep canvas rendering independent from ordinary React rendering.

Avoid unnecessary component re-renders.

Clean up PixiJS resources and event listeners when components unmount.

---

# 16. ORDER TRACKING

Customers must be able to track their orders.

Create a clear shipment timeline.

Example:

Order Placed
     |
Order Confirmed
     |
Warehouse Allocated
     |
Packed
     |
Dispatched
     |
In Transit
     |
Delivered

Display:

- Order ID.
- Product information.
- Selected warehouse.
- Delivery address.
- Estimated delivery date.
- Current shipment status.
- Shipment history.

Administrators should be able to see additional operational information.

The tracking timeline must use actual backend status records.

---

# 17. ANALYTICS AND REPORTING

Create an analytics section using Recharts.

Include:

### Order analytics

- Total orders.
- Orders by status.
- Orders over time.
- Average order value.

### Warehouse analytics

- Orders allocated per warehouse.
- Inventory distribution.
- Warehouse fulfilment comparison.

### Inventory analytics

- Total stock.
- Low-stock products.
- Product-wise availability.
- Stock movement.

### Dispatch analytics

- Active dispatches.
- Completed deliveries.
- Pending shipments.
- Dispatch status distribution.

Use actual database records.

Advanced ML-based demand forecasting is not part of this MVP.

A basic low-stock report is sufficient.

---

# 18. STATE MANAGEMENT

Use Zustand for frontend state management.

Create separate stores where appropriate:

- Authentication Store.
- Shopping Cart Store.
- UI Store.
- Dashboard Filter Store.
- Warehouse Visualization Store.
- Order Interaction Store.

Manage:

- Current authenticated user.
- Cart contents.
- Sidebar state.
- Selected warehouse.
- Selected order.
- Selected dispatch.
- Active filters.
- Visualization interactions.

Do not unnecessarily duplicate backend-owned data.

Server data must be retrieved from the backend through the API layer.

Use appropriate authentication token storage and avoid exposing sensitive information.

---

# 19. ANIMATIONS

Use Framer Motion for:

- Sidebar transitions.
- Slide-in panels.
- Modals.
- Dropdowns.
- Toast notifications.
- Page transitions where appropriate.
- Shipment status transitions.

Use PixiJS for:

- Warehouse node animations.
- Route rendering.
- Shipment movement.
- Visualization interactions.

Animations must remain subtle, professional and responsive.

Respect reduced-motion preferences.

---

# 20. DATA MODELS AND DATABASE

Use MySQL with a normalized relational schema.

The MVP requires the following tables:

1. users
2. categories
3. products
4. warehouses
5. inventory
6. inventory_transactions
7. orders
8. order_items
9. warehouse_allocations
10. allocation_evaluations
11. dispatches
12. delivery_tracking

## Important relationships

- One category can have many products.
- One product can have inventory in multiple warehouses.
- One warehouse can store multiple products.
- One customer can place multiple orders.
- One order can contain multiple order items.
- One order has an allocation decision.
- One order can generate a dispatch.
- One dispatch can have multiple tracking updates.

Use proper primary keys, foreign keys, unique constraints and indexes.

Use decimal database types for monetary values.

Use timestamps for important operational events.

Create the ER diagram before implementing the entities.

---

# 21. BACKEND ARCHITECTURE

Use a modular monolithic Spring Boot application.

Suggested package structure:

```text
backend/
  src/main/java/
    com/supplychain/
      config/
      security/
      auth/
      user/
      category/
      product/
      warehouse/
      inventory/
      order/
      allocation/
      dispatch/
      tracking/
      analytics/
      common/
      exception/
```

Follow:

Controller → Service → Repository → Database.

Requirements:

- DTO-based API communication.
- Centralized exception handling.
- Request validation.
- JWT authentication.
- Role-based access.
- Database transactions.
- Proper HTTP status codes.
- Pagination and filtering.
- Swagger documentation.

Do not put business logic in controllers.

---

# 22. FRONTEND ARCHITECTURE

Use Next.js App Router with TypeScript.

Suggested structure:

```text
frontend/
  src/
    app/
      (customer)/
      admin/
      login/
      register/

    components/
      ui/
      layout/
      products/
      inventory/
      warehouses/
      orders/
      dispatch/
      analytics/
      visualization/

    stores/
      auth/
      cart/
      ui/
      warehouse/
      orders/

    services/
      api/
      auth/
      products/
      inventory/
      warehouses/
      orders/
      dispatch/

    hooks/
    types/
    utils/
    lib/
```

Use:

- Reusable components.
- Strict TypeScript types.
- Protected layouts.
- Centralized API configuration.
- Proper loading states.
- Error handling.
- Empty states.
- Form validation.
- Responsive design.

Use client components only where interactivity requires them.

---

# 23. API SPECIFICATION

Create REST APIs.

### Authentication

- POST `/api/auth/register`
- POST `/api/auth/login`
- GET `/api/auth/me`

### Products

- GET `/api/products`
- POST `/api/products`
- GET `/api/products/{id}`
- PUT `/api/products/{id}`
- DELETE `/api/products/{id}`

### Warehouses

- GET `/api/warehouses`
- POST `/api/warehouses`
- GET `/api/warehouses/{id}`
- PUT `/api/warehouses/{id}`
- DELETE `/api/warehouses/{id}`

### Inventory

- GET `/api/inventory`
- POST `/api/inventory`
- PUT `/api/inventory/{id}`
- GET `/api/inventory/low-stock`
- GET `/api/inventory/warehouse/{warehouseId}`

### Orders

- POST `/api/orders`
- GET `/api/orders`
- GET `/api/orders/{id}`
- GET `/api/orders/my-orders`
- PATCH `/api/orders/{id}/cancel`

### Allocation

- GET `/api/allocations`
- GET `/api/allocations/{orderId}`
- GET `/api/allocations/{orderId}/evaluations`

### Dispatch

- GET `/api/dispatches`
- GET `/api/dispatches/{id}`
- PATCH `/api/dispatches/{id}/status`

### Tracking

- GET `/api/tracking/order/{orderId}`
- GET `/api/tracking/dispatch/{dispatchId}`

### Analytics

- GET `/api/analytics/dashboard`
- GET `/api/analytics/orders`
- GET `/api/analytics/warehouses`
- GET `/api/analytics/inventory`
- GET `/api/analytics/dispatches`

Adjust endpoints if necessary, while maintaining consistent API contracts.

---

# 24. SAMPLE DATA AND DEMONSTRATION

The application must be usable immediately after setup.

Create a database seeding mechanism.

Initial sample data:

- 3 warehouses.
- 10–15 products.
- 5 product categories.
- 10 customer accounts.
- 1 administrator.
- Multiple inventory records.
- At least 15 historical orders.
- Multiple dispatch records.
- Different shipment statuses.
- Low-stock scenarios.

Use INR for all financial values.

Provide meaningful demonstration scenarios.

### Required demonstration scenario

Customer orders:

- 10 smartphones.
- 5 wireless headphones.

Hubli has sufficient inventory.

Bengaluru has sufficient inventory.

Mysuru has insufficient smartphone inventory.

The system must:

1. Reject Mysuru.
2. Evaluate Hubli and Bengaluru.
3. Calculate weighted scores.
4. Select the best warehouse.
5. Reserve inventory.
6. Confirm the order.
7. Create a dispatch.
8. Display the decision details.

Also include a scenario where no warehouse has enough inventory.

In that situation, the system must reject the order or clearly mark it as requiring intervention. It must never silently allocate insufficient stock.

---

# 25. TESTING REQUIREMENTS

Use Vitest for frontend utilities and warehouse decision-engine unit tests.

Test:

- Warehouse eligibility.
- Inventory sufficiency.
- Score normalization.
- Weighted score calculation.
- Warehouse ranking.
- Tie-breaking.
- Cart calculations.
- Inventory validation.
- Shipment status transitions.

Backend tests must cover:

- Authentication.
- Authorization.
- Inventory transactions.
- Concurrent reservations.
- Order creation.
- Warehouse allocation.
- Dispatch creation.
- Status updates.
- Cancellation and inventory release.

Use integration tests with a real MySQL-compatible test environment where possible.

Do not claim the system is complete without running tests.

---

# 26. NON-FUNCTIONAL REQUIREMENTS

### Performance

- Efficient API requests.
- Pagination for large tables.
- Avoid unnecessary React re-renders.
- Efficient PixiJS rendering.
- Proper cleanup of graphical resources.

### Security

- Secure password hashing.
- JWT authentication.
- Role-based authorization.
- Request validation.
- Protected API endpoints.
- No hardcoded production credentials.

### Reliability

- Prevent negative inventory.
- Prevent duplicate reservations.
- Maintain transaction consistency.
- Record important inventory and shipment changes.
- Handle API failures gracefully.

### Usability

- Clear error messages.
- Consistent design.
- Responsive interfaces.
- Accessible controls.
- Clear operational status indicators.

---

# 27. EXPLICITLY OUT OF SCOPE FOR MVP

The following features must not be implemented in the initial version:

- Machine learning demand forecasting.
- Random Forest or Linear Regression forecasting models.
- Python FastAPI ML service.
- LLM integration.
- Real-time GPS tracking.
- External courier API integrations.
- Actual payment gateway.
- IoT sensors.
- Physical warehouse automation.
- Robot-assisted inventory movement.
- Live traffic integration.
- Advanced route optimization.
- Multi-tenant SaaS architecture.
- Microservices infrastructure.

These can be considered in future project phases.

Do not create placeholder functionality and label it as implemented AI.

The MVP should be technically honest about the features it contains.

---

# 28. DEFINITION OF DONE — MVP

The MVP is considered complete when all the following conditions are satisfied.

1. Loading the application opens the customer-facing interface.
2. Customers can register and log in.
3. Administrators can log in separately.
4. Administrators can manage products.
5. Administrators can manage warehouses.
6. Administrators can manage inventory.
7. Customers can browse products.
8. Customers can add products to their cart.
9. Customers can place orders.
10. The system checks inventory across multiple warehouses.
11. Ineligible warehouses are rejected.
12. Eligible warehouses are compared using weighted scoring.
13. The most suitable warehouse is selected automatically.
14. Allocation decisions are stored and explainable.
15. Inventory is reserved transactionally.
16. Orders are confirmed only after successful reservation.
17. Dispatch records are automatically generated.
18. Administrators can update dispatch statuses.
19. Customers can track their shipments.
20. The supply chain map visualizes warehouses and shipment routes.
21. Analytics reflect actual application data.
22. All important business logic has automated tests.
23. The application runs reliably using the documented setup instructions.

---

# 29. DEVELOPMENT EXECUTION INSTRUCTIONS FOR AI CODING AGENTS

You must follow a phased development process.

Do not generate the complete application in one response.

## Phase 0 — Architecture

Before writing code, produce:

- Complete system architecture.
- Database schema.
- ER diagram.
- Backend folder structure.
- Frontend folder structure.
- API specification.
- Order lifecycle.
- Warehouse allocation algorithm.
- PixiJS visualization architecture.

Wait for approval.

## Phase 1 — Frontend foundation

Configure:

- Next.js.
- TypeScript.
- Tailwind CSS.
- Zustand.
- Framer Motion.
- PixiJS.
- ESLint.
- Prettier.
- Vitest.

Create the design system and shared components.

## Phase 2 — Backend foundation

Configure Spring Boot and MySQL.

Implement:

- Database entities.
- Repositories.
- Authentication.
- JWT security.
- Role-based authorization.

## Phase 3 — Management modules

Implement:

- Product management.
- Category management.
- Warehouse management.
- Inventory management.

## Phase 4 — Customer ordering

Implement:

- Registration.
- Login.
- Product catalogue.
- Product details.
- Cart.
- Checkout.
- Order placement.

## Phase 5 — Optimization engine

Implement:

- Inventory validation.
- Warehouse eligibility.
- Weighted scoring.
- Warehouse selection.
- Explainable allocation.
- Transaction-safe stock reservation.

## Phase 6 — Dispatch and tracking

Implement:

- Automatic dispatch generation.
- Shipment status management.
- Tracking history.
- Customer order tracking.

## Phase 7 — Visualization

Implement:

- Interactive warehouse map.
- Warehouse nodes.
- Customer locations.
- Connection lines.
- Shipment animations.

Connect all visualization elements to real backend data.

## Phase 8 — Dashboard and analytics

Implement the administrator control tower and operational analytics.

## Phase 9 — Testing and integration

Verify all application workflows.

Fix bugs.

Run unit and integration tests.

Verify database consistency.

Prepare demonstration accounts and sample data.

Prepare final README documentation.

At the end of every phase:

1. Explain completed work.
2. List created and modified files.
3. Run relevant tests.
4. Report outstanding issues.
5. Update the development checklist.
6. Wait for explicit approval before moving to the next phase.

---

# 30. FINAL DEVELOPMENT PRINCIPLES

The following principles must be followed throughout development.

- Build a real working application, not a static prototype.
- Do not use mock data in place of implemented backend functionality.
- Do not hardcode analytics.
- Do not generate unnecessary features.
- Do not change the selected technology stack.
- Do not introduce unnecessary infrastructure.
- Do not compromise database integrity.
- Do not bypass authentication.
- Do not generate random warehouse allocation decisions.
- Do not use LLMs for deterministic inventory decisions.
- Do not claim to have implemented machine learning.
- Keep the code modular and maintainable.
- Explain important architectural decisions.
- Maintain consistent frontend/backend API contracts.
- Test all critical business logic.

If a requirement is ambiguous, explain the ambiguity and propose a reasonable implementation before proceeding.

---

# FINAL OBJECTIVE

Deliver a polished, functional and demonstrable academic MVP of an AI-Powered Supply Chain Optimization & Automated Dispatch System.

The final demonstration must allow a professor to observe the complete lifecycle of a customer order, understand how the system evaluates different warehouses, see how inventory is reserved and follow the resulting dispatch.

The application must demonstrate genuine automated supply chain decision-making through a professional enterprise interface.

**Prioritize functional correctness, explainable optimization, reliable inventory management and an impressive interactive demonstration over unnecessary complexity.**