package com.swiftdrop.dispatch.api;

import com.swiftdrop.dispatch.dto.ReportSummaryResponse;
import com.swiftdrop.dispatch.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dispatch/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('DISPATCHER','ADMIN')")
public class ReportController {

    private final ReportService service;

    /** One call for all three charts: deliveries per day, success/failure rate, deliveries per zone. */
    @GetMapping("/summary")
    public ReportSummaryResponse summary(@RequestParam(defaultValue = "7") int days) {
        return service.summary(days);
    }
}