-- Dispatch records began in this version. Orders confirmed before it have reserved stock but no
-- dispatch, so they get what the application would have created for them at confirmation time.

INSERT INTO dispatches (order_id, warehouse_id, tracking_reference, status, estimated_delivery_date, created_at)
SELECT o.id,
       o.selected_warehouse_id,
       CONCAT('TRK-', YEAR(o.confirmed_at), '-', LPAD(o.id, 6, '0')),
       'PENDING',
       -- Same rule as DispatchService: whole days of estimated time in IST, plus a day for Standard.
       DATE_ADD(DATE(DATE_ADD(o.confirmed_at, INTERVAL 330 MINUTE)),
                INTERVAL CEIL(e.eta_hours / 24) + IF(o.priority = 'STANDARD', 1, 0) DAY),
       o.confirmed_at
FROM orders o
JOIN warehouse_allocations a ON a.order_id = o.id AND a.outcome = 'ALLOCATED'
JOIN allocation_evaluations e ON e.allocation_id = a.id AND e.decision = 'SELECTED'
LEFT JOIN dispatches d ON d.order_id = o.id
WHERE o.status = 'CONFIRMED' AND d.id IS NULL;

INSERT INTO delivery_tracking (dispatch_id, from_status, to_status, updated_by, note, created_at)
SELECT d.id, NULL, 'PENDING', NULL, 'Dispatch created', d.created_at
FROM dispatches d
LEFT JOIN delivery_tracking t ON t.dispatch_id = d.id
WHERE t.id IS NULL;
