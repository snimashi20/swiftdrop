package com.swiftdrop.parcel.api;

import com.swiftdrop.parcel.domain.ParcelStatus;
import com.swiftdrop.parcel.dto.CreateParcelRequest;
import com.swiftdrop.parcel.dto.ParcelDetailResponse;
import com.swiftdrop.parcel.dto.ParcelResponse;
import com.swiftdrop.parcel.service.ParcelService;
import com.swiftdrop.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/parcels")
@RequiredArgsConstructor
public class ParcelController {

    private final ParcelService parcelService;

    /** A signed-in customer books a parcel. */
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ParcelResponse> create(@Valid @RequestBody CreateParcelRequest request,
                                                 @AuthenticationPrincipal AuthenticatedUser currentUser) {
        ParcelResponse response = parcelService.createForCustomer(currentUser.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** My parcels: GET /api/v1/parcels?page=0&size=10&status=CREATED (newest first). */
    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public Page<ParcelDetailResponse> list(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "10") int size,
                                           @RequestParam(required = false) ParcelStatus status,
                                           @AuthenticationPrincipal AuthenticatedUser currentUser) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50),
                Sort.by("createdAt").descending());
        return parcelService.listForCustomer(currentUser.id(), status, pageable);
    }

    /** One of my parcels. Someone else's parcel gives 404. */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ParcelDetailResponse get(@PathVariable Long id,
                                    @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return parcelService.getForCustomer(currentUser.id(), id);
    }
}