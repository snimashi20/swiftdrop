package com.swiftdrop.dispatch.api;

import com.swiftdrop.dispatch.dto.CourierProfileCreateRequest;
import com.swiftdrop.dispatch.dto.CourierProfileResponse;
import com.swiftdrop.dispatch.dto.CourierProfileUpdateRequest;
import com.swiftdrop.dispatch.service.CourierProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dispatch/couriers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('DISPATCHER','ADMIN')")
public class CourierProfileController {

    private final CourierProfileService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourierProfileResponse create(@Valid @RequestBody CourierProfileCreateRequest request) {
        return service.create(request);
    }

    @GetMapping
    public Page<CourierProfileResponse> list(
            @RequestParam(required = false) Long zoneId,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return service.list(zoneId, active, pageable);
    }

    @GetMapping("/{id}")
    public CourierProfileResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PatchMapping("/{id}")
    public CourierProfileResponse update(@PathVariable Long id,
                                         @Valid @RequestBody CourierProfileUpdateRequest request) {
        return service.update(id, request);
    }
}