package com.swiftdrop.dispatch.repository;

/** Projection: number of active (not finished) parcels of one courier profile. */
public interface CourierLoad {
    Long getCourierId();

    Long getActiveCount();
}