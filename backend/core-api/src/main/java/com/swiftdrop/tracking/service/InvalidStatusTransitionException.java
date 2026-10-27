package com.swiftdrop.tracking.service;

import com.swiftdrop.common.exception.ConflictException;
import com.swiftdrop.parcel.domain.ParcelStatus;

public class InvalidStatusTransitionException extends ConflictException {

    public InvalidStatusTransitionException(ParcelStatus from, ParcelStatus to) {
        super("Cannot change status from " + from + " to " + to);
    }
}
