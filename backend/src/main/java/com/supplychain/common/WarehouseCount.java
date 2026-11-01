package com.supplychain.common;

/** A per-warehouse count, as returned by grouped queries. */
public record WarehouseCount(long warehouseId, long count) {}
