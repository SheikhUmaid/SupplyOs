-- SupplyChain OS schema. Design and ER diagram: docs/ARCHITECTURE.md, section 4.
-- All timestamps are UTC. Physical stock = available_quantity + reserved_quantity.

CREATE TABLE users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    full_name     VARCHAR(120) NOT NULL,
    email         VARCHAR(190) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    phone         VARCHAR(20)  NULL,
    role          ENUM ('ADMIN', 'CUSTOMER') NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE categories (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_categories_name UNIQUE (name)
);

CREATE TABLE products (
    id          BIGINT         NOT NULL AUTO_INCREMENT,
    category_id BIGINT         NOT NULL,
    name        VARCHAR(160)   NOT NULL,
    sku         VARCHAR(64)    NOT NULL,
    description TEXT           NULL,
    price       DECIMAL(12, 2) NOT NULL,
    image_url   VARCHAR(500)   NULL,
    status      ENUM ('ACTIVE', 'INACTIVE') NOT NULL,
    created_at  DATETIME(6)    NOT NULL,
    updated_at  DATETIME(6)    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_products_sku UNIQUE (sku),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT chk_products_price CHECK (price >= 0),
    INDEX idx_products_status_name (status, name)
);

CREATE TABLE warehouses (
    id                 BIGINT         NOT NULL AUTO_INCREMENT,
    name               VARCHAR(120)   NOT NULL,
    address            VARCHAR(255)   NOT NULL,
    city               VARCHAR(100)   NOT NULL,
    state              VARCHAR(100)   NOT NULL,
    postal_code        VARCHAR(10)    NOT NULL,
    latitude           DECIMAL(9, 6)  NOT NULL,
    longitude          DECIMAL(9, 6)  NOT NULL,
    contact_number     VARCHAR(20)    NOT NULL,
    base_delivery_cost DECIMAL(10, 2) NOT NULL,
    cost_per_km        DECIMAL(8, 2)  NOT NULL,
    handling_hours     INT            NOT NULL,
    status             ENUM ('ACTIVE', 'INACTIVE') NOT NULL,
    created_at         DATETIME(6)    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_warehouses_name UNIQUE (name),
    CONSTRAINT chk_warehouses_latitude CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT chk_warehouses_longitude CHECK (longitude BETWEEN -180 AND 180),
    CONSTRAINT chk_warehouses_base_cost CHECK (base_delivery_cost >= 0),
    CONSTRAINT chk_warehouses_cost_per_km CHECK (cost_per_km >= 0),
    CONSTRAINT chk_warehouses_handling CHECK (handling_hours >= 0)
);

CREATE TABLE inventory (
    id                 BIGINT      NOT NULL AUTO_INCREMENT,
    product_id         BIGINT      NOT NULL,
    warehouse_id       BIGINT      NOT NULL,
    available_quantity INT         NOT NULL,
    reserved_quantity  INT         NOT NULL DEFAULT 0,
    reorder_level      INT         NOT NULL DEFAULT 0,
    last_updated       DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    -- Also the path used to lock stock rows during allocation.
    CONSTRAINT uk_inventory_product_warehouse UNIQUE (product_id, warehouse_id),
    CONSTRAINT fk_inventory_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_inventory_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT chk_inventory_available CHECK (available_quantity >= 0),
    CONSTRAINT chk_inventory_reserved CHECK (reserved_quantity >= 0),
    CONSTRAINT chk_inventory_reorder CHECK (reorder_level >= 0),
    INDEX idx_inventory_warehouse (warehouse_id)
);

CREATE TABLE orders (
    id                    BIGINT         NOT NULL AUTO_INCREMENT,
    order_number          VARCHAR(32)    NOT NULL,
    customer_id           BIGINT         NOT NULL,
    selected_warehouse_id BIGINT         NULL,
    status                ENUM ('PENDING', 'CONFIRMED', 'PROCESSING', 'DISPATCHED', 'DELIVERED', 'CANCELLED') NOT NULL,
    priority              ENUM ('STANDARD', 'EXPRESS') NOT NULL,
    payment_status        ENUM ('UNPAID', 'PAID_SIMULATED', 'REFUNDED_SIMULATED') NOT NULL,
    total_amount          DECIMAL(12, 2) NOT NULL,
    delivery_name         VARCHAR(120)   NOT NULL,
    delivery_phone        VARCHAR(20)    NOT NULL,
    delivery_address      VARCHAR(255)   NOT NULL,
    delivery_city         VARCHAR(100)   NOT NULL,
    delivery_state        VARCHAR(100)   NOT NULL,
    delivery_postal_code  VARCHAR(10)    NOT NULL,
    delivery_latitude     DECIMAL(9, 6)  NOT NULL,
    delivery_longitude    DECIMAL(9, 6)  NOT NULL,
    idempotency_key       VARCHAR(64)    NOT NULL,
    created_at            DATETIME(6)    NOT NULL,
    confirmed_at          DATETIME(6)    NULL,
    cancelled_at          DATETIME(6)    NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_orders_number UNIQUE (order_number),
    -- A double-clicked checkout cannot create two orders.
    CONSTRAINT uk_orders_customer_idempotency UNIQUE (customer_id, idempotency_key),
    CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES users (id),
    CONSTRAINT fk_orders_warehouse FOREIGN KEY (selected_warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT chk_orders_total CHECK (total_amount >= 0),
    INDEX idx_orders_customer_created (customer_id, created_at),
    INDEX idx_orders_status_created (status, created_at)
);

CREATE TABLE order_items (
    id         BIGINT         NOT NULL AUTO_INCREMENT,
    order_id   BIGINT         NOT NULL,
    product_id BIGINT         NOT NULL,
    quantity   INT            NOT NULL,
    unit_price DECIMAL(12, 2) NOT NULL,
    subtotal   DECIMAL(12, 2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id),
    -- Restricts deletion: products on past orders can only be deactivated.
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE RESTRICT,
    CONSTRAINT chk_order_items_quantity CHECK (quantity > 0),
    CONSTRAINT chk_order_items_unit_price CHECK (unit_price >= 0),
    CONSTRAINT chk_order_items_subtotal CHECK (subtotal >= 0)
);

-- Append-only stock ledger: the application never updates or deletes these rows.
CREATE TABLE inventory_transactions (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    inventory_id    BIGINT      NOT NULL,
    order_id        BIGINT      NULL,
    performed_by    BIGINT      NULL, -- NULL means the system.
    type            ENUM ('RESTOCK', 'ADJUSTMENT', 'RESERVE', 'RELEASE', 'SHIP') NOT NULL,
    available_delta INT         NOT NULL,
    reserved_delta  INT         NOT NULL,
    available_after INT         NOT NULL,
    reserved_after  INT         NOT NULL,
    note            VARCHAR(255) NULL,
    created_at      DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_inv_tx_inventory FOREIGN KEY (inventory_id) REFERENCES inventory (id),
    CONSTRAINT fk_inv_tx_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_inv_tx_user FOREIGN KEY (performed_by) REFERENCES users (id),
    INDEX idx_inv_tx_inventory_created (inventory_id, created_at)
);

CREATE TABLE warehouse_allocations (
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    order_id              BIGINT       NOT NULL,
    attempt_no            INT          NOT NULL,
    outcome               ENUM ('ALLOCATED', 'NO_ELIGIBLE_WAREHOUSE') NOT NULL,
    selected_warehouse_id BIGINT       NULL,
    -- Weights in force for this decision, so it stays explainable after a configuration change.
    weight_distance       DECIMAL(4, 3) NOT NULL,
    weight_cost           DECIMAL(4, 3) NOT NULL,
    weight_inventory      DECIMAL(4, 3) NOT NULL,
    weight_priority       DECIMAL(4, 3) NOT NULL,
    explanation           TEXT         NOT NULL,
    allocated_at          DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_allocations_order_attempt UNIQUE (order_id, attempt_no),
    CONSTRAINT fk_allocations_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_allocations_warehouse FOREIGN KEY (selected_warehouse_id) REFERENCES warehouses (id),
    -- A warehouse is recorded exactly when the outcome says one was chosen.
    CONSTRAINT chk_allocations_selection CHECK (
        (outcome = 'ALLOCATED' AND selected_warehouse_id IS NOT NULL)
            OR (outcome = 'NO_ELIGIBLE_WAREHOUSE' AND selected_warehouse_id IS NULL))
);

CREATE TABLE allocation_evaluations (
    id               BIGINT         NOT NULL AUTO_INCREMENT,
    allocation_id    BIGINT         NOT NULL,
    warehouse_id     BIGINT         NOT NULL,
    eligible         BOOLEAN        NOT NULL,
    rejection_reason VARCHAR(500)   NULL,
    distance_km      DECIMAL(8, 2)  NOT NULL,
    delivery_cost    DECIMAL(10, 2) NOT NULL,
    eta_hours        DECIMAL(8, 2)  NOT NULL,
    -- Scoring columns are NULL for rejected warehouses, which are never scored.
    norm_distance    DECIMAL(7, 6)  NULL,
    norm_cost        DECIMAL(7, 6)  NULL,
    inventory_factor DECIMAL(7, 6)  NULL,
    priority_factor  DECIMAL(7, 6)  NULL,
    final_score      DECIMAL(7, 6)  NULL,
    rank_position    INT            NULL,
    decision         ENUM ('SELECTED', 'NOT_SELECTED', 'REJECTED') NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_evaluations_allocation_warehouse UNIQUE (allocation_id, warehouse_id),
    CONSTRAINT fk_evaluations_allocation FOREIGN KEY (allocation_id) REFERENCES warehouse_allocations (id),
    CONSTRAINT fk_evaluations_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    -- Stock availability is a hard rule: an ineligible warehouse can never be selected.
    CONSTRAINT chk_evaluations_selected_eligible CHECK (decision <> 'SELECTED' OR eligible = TRUE)
);

CREATE TABLE dispatches (
    id                      BIGINT      NOT NULL AUTO_INCREMENT,
    order_id                BIGINT      NOT NULL,
    warehouse_id            BIGINT      NOT NULL,
    tracking_reference      VARCHAR(32) NOT NULL,
    status                  ENUM ('PENDING', 'PACKED', 'DISPATCHED', 'IN_TRANSIT', 'DELIVERED', 'CANCELLED') NOT NULL,
    estimated_delivery_date DATE        NOT NULL,
    dispatch_date           DATETIME(6) NULL,
    delivery_date           DATETIME(6) NULL,
    created_at              DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_dispatches_order UNIQUE (order_id),
    CONSTRAINT uk_dispatches_tracking UNIQUE (tracking_reference),
    CONSTRAINT fk_dispatches_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_dispatches_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    INDEX idx_dispatches_status_created (status, created_at)
);

CREATE TABLE delivery_tracking (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    dispatch_id BIGINT      NOT NULL,
    from_status ENUM ('PENDING', 'PACKED', 'DISPATCHED', 'IN_TRANSIT', 'DELIVERED', 'CANCELLED') NULL,
    to_status   ENUM ('PENDING', 'PACKED', 'DISPATCHED', 'IN_TRANSIT', 'DELIVERED', 'CANCELLED') NOT NULL,
    updated_by  BIGINT      NULL, -- NULL means the system.
    note        VARCHAR(255) NULL,
    created_at  DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_tracking_dispatch FOREIGN KEY (dispatch_id) REFERENCES dispatches (id),
    CONSTRAINT fk_tracking_user FOREIGN KEY (updated_by) REFERENCES users (id),
    INDEX idx_tracking_dispatch_created (dispatch_id, created_at)
);
