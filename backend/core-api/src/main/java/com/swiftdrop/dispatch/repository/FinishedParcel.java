package com.swiftdrop.dispatch.repository;

import com.swiftdrop.parcel.domain.ParcelStatus;

import java.time.Instant;

/** Projection for reports: one finished parcel (delivered, failed or cancelled). */
public interface FinishedParcel {
    ParcelStatus getStatus();

    Instant getFinishedAt();

    String getDistrict();
}