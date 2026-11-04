package com.swiftdrop.dispatch.api;

import com.swiftdrop.dispatch.dto.CourierCandidateResponse;
import com.swiftdrop.dispatch.service.DispatchLookupService;
import com.swiftdrop.dispatch.service.ZoneLookup;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dispatch/lookups")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('DISPATCHER','ADMIN')")
public class DispatchLookupController {

    private final DispatchLookupService service;

    @GetMapping("/zones")
    public List<ZoneLookup.ZoneOption> zones() {
        return service.zones();
    }

    @GetMapping("/courier-candidates")
    public List<CourierCandidateResponse> courierCandidates() {
        return service.courierCandidates();
    }
}