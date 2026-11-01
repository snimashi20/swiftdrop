package com.swiftdrop.dispatch.api;

import com.swiftdrop.dispatch.dto.AssignParcelRequest;
import com.swiftdrop.dispatch.dto.CourierWorkloadResponse;
import com.swiftdrop.dispatch.dto.ParcelAssignmentResponse;
import com.swiftdrop.dispatch.dto.UnassignedParcelResponse;
import com.swiftdrop.dispatch.service.DispatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/dispatch")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('DISPATCHER','ADMIN')")
public class DispatchController {

    private final DispatchService service;

    @GetMapping("/parcels/unassigned")
    public Page<UnassignedParcelResponse> unassigned(
            @RequestParam(required = false) String district,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return service.listUnassigned(district, from, to, pageable);
    }

    /** Assign a parcel, or reassign it if it already has a courier. */
    @PutMapping("/parcels/{parcelId}/assign")
    public ParcelAssignmentResponse assign(@PathVariable Long parcelId,
                                           @Valid @RequestBody AssignParcelRequest request,
                                           Authentication authentication) {
        return service.assign(parcelId, request.courierProfileId(), currentUserId(authentication));
    }

    @GetMapping("/couriers/workload")
    public List<CourierWorkloadResponse> workload(@RequestParam(required = false) Long zoneId,
                                                  @RequestParam(required = false) Boolean active) {
        return service.workload(zoneId, active);
    }

    /** TODO(M8): read the user id from the JWT once login is merged; null is stored until then. */
    private Long currentUserId(Authentication authentication) {
        try {
            return authentication == null ? null : Long.valueOf(authentication.getName());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}