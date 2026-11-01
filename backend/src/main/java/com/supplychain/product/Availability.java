package com.supplychain.product;

/** What customers see instead of stock counts, across all active warehouses. */
public enum Availability {
    IN_STOCK,
    LOW,
    OUT;

    public static Availability of(long totalAvailable, long totalReorderLevel) {
        if (totalAvailable <= 0) {
            return OUT;
        }
        return totalAvailable < totalReorderLevel ? LOW : IN_STOCK;
    }
}
