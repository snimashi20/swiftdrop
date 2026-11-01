package com.swiftdrop.dispatch.service;

import com.swiftdrop.common.exception.BadRequestException;
import com.swiftdrop.dispatch.dto.ReportSummaryResponse;
import com.swiftdrop.dispatch.dto.ReportSummaryResponse.DailyDeliveries;
import com.swiftdrop.dispatch.dto.ReportSummaryResponse.Outcomes;
import com.swiftdrop.dispatch.dto.ReportSummaryResponse.ZoneDeliveries;
import com.swiftdrop.dispatch.repository.FinishedParcel;
import com.swiftdrop.dispatch.repository.ReportParcelRepository;
import com.swiftdrop.parcel.domain.ParcelStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final ZoneId SRI_LANKA = ZoneId.of("Asia/Colombo");
    /** Looked up by name so this keeps compiling before CANCELLED is merged. */
    private static final Set<String> FINISHED = Set.of("DELIVERED", "FAILED", "CANCELLED");

    private final ReportParcelRepository repository;

    @Transactional(readOnly = true)
    public ReportSummaryResponse summary(int days) {
        return summary(days, LocalDate.now(SRI_LANKA));
    }

    /** Package-private so tests can fix "today". */
    ReportSummaryResponse summary(int days, LocalDate today) {
        if (days != 7 && days != 30) {
            throw new BadRequestException("days must be 7 or 30");
        }
        LocalDate from = today.minusDays(days - 1L);
        Instant since = from.atStartOfDay(SRI_LANKA).toInstant();

        List<ParcelStatus> finishedStatuses = Arrays.stream(ParcelStatus.values())
                .filter(s -> FINISHED.contains(s.name()))
                .toList();
        List<FinishedParcel> rows = repository.findFinishedSince(finishedStatuses, since);

        Map<LocalDate, Counter> daily = new LinkedHashMap<>();
        for (int i = 0; i < days; i++) {
            daily.put(from.plusDays(i), new Counter());
        }
        Map<String, Counter> perZone = new TreeMap<>();
        Counter total = new Counter();

        for (FinishedParcel row : rows) {
            LocalDate date = row.getFinishedAt().atZone(SRI_LANKA).toLocalDate();
            Counter day = daily.get(date);
            if (day == null) {
                continue; // outside the requested range
            }
            String status = row.getStatus().name();
            day.add(status);
            total.add(status);
            String district = row.getDistrict() == null ? "Unknown" : row.getDistrict();
            perZone.computeIfAbsent(district, d -> new Counter()).add(status);
        }

        List<DailyDeliveries> dailyList = daily.entrySet().stream()
                .map(e -> new DailyDeliveries(e.getKey(), e.getValue().delivered,
                        e.getValue().failed, e.getValue().cancelled))
                .toList();
        List<ZoneDeliveries> zoneList = perZone.entrySet().stream()
                .map(e -> new ZoneDeliveries(e.getKey(), e.getValue().delivered,
                        e.getValue().failed, e.getValue().cancelled))
                .toList();

        long attempted = total.delivered + total.failed;
        Outcomes outcomes = new Outcomes(total.delivered, total.failed, total.cancelled,
                percent(total.delivered, attempted), percent(total.failed, attempted));

        return new ReportSummaryResponse(days, from, today, dailyList, outcomes, zoneList);
    }

    private static Double percent(long part, long whole) {
        return whole == 0 ? null : Math.round(part * 1000.0 / whole) / 10.0;
    }

    private static final class Counter {
        long delivered;
        long failed;
        long cancelled;

        void add(String status) {
            switch (status) {
                case "DELIVERED" -> delivered++;
                case "FAILED" -> failed++;
                case "CANCELLED" -> cancelled++;
                default -> { }
            }
        }
    }
}