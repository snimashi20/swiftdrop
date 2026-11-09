package com.swiftdrop.dispatch.dto;

import java.time.LocalDate;
import java.util.List;

public record ReportSummaryResponse(
        int days,
        LocalDate from,
        LocalDate to,
        List<DailyDeliveries> daily,
        Outcomes outcomes,
        List<ZoneDeliveries> perZone
) {

    /** Parcels that reached a final status on this day. */
    public record DailyDeliveries(LocalDate date, long delivered, long failed, long cancelled) {}

    /**
     * Success and failure rates are calculated from delivered + failed only.
     * Cancelled parcels are counted separately. A rate is null when there is no data.
     */
    public record Outcomes(long delivered, long failed, long cancelled,
                           Double successRatePercent, Double failureRatePercent) {}

    /** Zone = pickup district for now (one zone = one district). */
    public record ZoneDeliveries(String district, long delivered, long failed, long cancelled) {}
}