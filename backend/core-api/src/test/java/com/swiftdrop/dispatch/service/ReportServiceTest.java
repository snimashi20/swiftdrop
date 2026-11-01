package com.swiftdrop.dispatch.service;

import com.swiftdrop.common.exception.BadRequestException;
import com.swiftdrop.dispatch.dto.ReportSummaryResponse;
import com.swiftdrop.dispatch.repository.FinishedParcel;
import com.swiftdrop.dispatch.repository.ReportParcelRepository;
import com.swiftdrop.parcel.domain.ParcelStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 10);
    private static final ZoneId SRI_LANKA = ZoneId.of("Asia/Colombo");

    @Mock ReportParcelRepository repository;
    @InjectMocks ReportService service;

    private static Instant noonOn(LocalDate date) {
        return date.atTime(12, 0).atZone(SRI_LANKA).toInstant();
    }

    private static FinishedParcel parcel(ParcelStatus status, LocalDate date, String district) {
        return new FinishedParcel() {
            public ParcelStatus getStatus() { return status; }
            public Instant getFinishedAt() { return noonOn(date); }
            public String getDistrict() { return district; }
        };
    }

    @Test
    void summary_returnsOneEntryPerDay_evenWithNoData() {
        when(repository.findFinishedSince(any(), any())).thenReturn(List.of());

        ReportSummaryResponse result = service.summary(7, TODAY);

        assertEquals(7, result.daily().size());
        assertEquals(TODAY.minusDays(6), result.daily().get(0).date());
        assertEquals(TODAY, result.daily().get(6).date());
        assertEquals(TODAY.minusDays(6), result.from());
        assertTrue(result.perZone().isEmpty());
    }

    @Test
    void summary_returnsThirtyDays_whenAsked() {
        when(repository.findFinishedSince(any(), any())).thenReturn(List.of());

        assertEquals(30, service.summary(30, TODAY).daily().size());
    }

    @Test
    void summary_countsParcelsOnTheDayTheyFinished() {
        when(repository.findFinishedSince(any(), any())).thenReturn(List.of(
                parcel(ParcelStatus.DELIVERED, TODAY, "Colombo"),
                parcel(ParcelStatus.DELIVERED, TODAY, "Colombo"),
                parcel(ParcelStatus.FAILED, TODAY, "Kandy"),
                parcel(ParcelStatus.DELIVERED, TODAY.minusDays(1), "Galle")));

        ReportSummaryResponse result = service.summary(7, TODAY);

        var last = result.daily().get(6);
        assertEquals(2, last.delivered());
        assertEquals(1, last.failed());
        var previous = result.daily().get(5);
        assertEquals(1, previous.delivered());
        assertEquals(0, previous.failed());
    }

    @Test
    void summary_calculatesSuccessAndFailureRates_fromDeliveredAndFailedOnly() {
        when(repository.findFinishedSince(any(), any())).thenReturn(List.of(
                parcel(ParcelStatus.DELIVERED, TODAY, "Colombo"),
                parcel(ParcelStatus.DELIVERED, TODAY, "Colombo"),
                parcel(ParcelStatus.DELIVERED, TODAY.minusDays(2), "Colombo"),
                parcel(ParcelStatus.FAILED, TODAY, "Kandy")));

        ReportSummaryResponse.Outcomes outcomes = service.summary(7, TODAY).outcomes();

        assertEquals(3, outcomes.delivered());
        assertEquals(1, outcomes.failed());
        assertEquals(75.0, outcomes.successRatePercent());
        assertEquals(25.0, outcomes.failureRatePercent());
    }

    @Test
    void summary_ratesAreNull_whenNothingFinished() {
        when(repository.findFinishedSince(any(), any())).thenReturn(List.of());

        ReportSummaryResponse.Outcomes outcomes = service.summary(7, TODAY).outcomes();

        assertNull(outcomes.successRatePercent());
        assertNull(outcomes.failureRatePercent());
    }

    @Test
    void summary_groupsByPickupDistrict_sortedByName() {
        when(repository.findFinishedSince(any(), any())).thenReturn(List.of(
                parcel(ParcelStatus.DELIVERED, TODAY, "Kandy"),
                parcel(ParcelStatus.DELIVERED, TODAY, "Colombo"),
                parcel(ParcelStatus.FAILED, TODAY, "Colombo")));

        List<ReportSummaryResponse.ZoneDeliveries> zones = service.summary(7, TODAY).perZone();

        assertEquals(2, zones.size());
        assertEquals("Colombo", zones.get(0).district());
        assertEquals(1, zones.get(0).delivered());
        assertEquals(1, zones.get(0).failed());
        assertEquals("Kandy", zones.get(1).district());
    }

    @Test
    void summary_ignoresParcelsOutsideTheRange() {
        when(repository.findFinishedSince(any(), any())).thenReturn(List.of(
                parcel(ParcelStatus.DELIVERED, TODAY.minusDays(8), "Colombo")));

        ReportSummaryResponse result = service.summary(7, TODAY);

        assertEquals(0, result.outcomes().delivered());
    }

    @Test
    void summary_countsCancelledSeparately_whenStatusExists() {
        assumeTrue(Arrays.stream(ParcelStatus.values()).anyMatch(s -> s.name().equals("CANCELLED")),
                "CANCELLED is not merged yet");
        ParcelStatus cancelled = ParcelStatus.valueOf("CANCELLED");
        when(repository.findFinishedSince(any(), any())).thenReturn(List.of(
                parcel(cancelled, TODAY, "Colombo"),
                parcel(ParcelStatus.DELIVERED, TODAY, "Colombo")));

        ReportSummaryResponse.Outcomes outcomes = service.summary(7, TODAY).outcomes();

        assertEquals(1, outcomes.cancelled());
        assertEquals(100.0, outcomes.successRatePercent(), "cancelled must not lower the success rate");
    }

    @Test
    void summary_rejectsOtherPeriods() {
        assertThrows(BadRequestException.class, () -> service.summary(14, TODAY));
        verify(repository, never()).findFinishedSince(any(), any());
    }
}