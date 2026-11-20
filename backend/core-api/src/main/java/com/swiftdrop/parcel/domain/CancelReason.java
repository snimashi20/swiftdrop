package com.swiftdrop.parcel.domain;

/** Why a customer cancelled their parcel. */
public enum CancelReason {
    BOOKED_BY_MISTAKE("Booked by mistake"),
    WRONG_DETAILS("Wrong address or receiver details"),
    FOUND_ANOTHER_OPTION("Found another option"),
    NO_LONGER_NEEDED("Delivery no longer needed"),
    OTHER("Other");

    private final String label;

    CancelReason(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
