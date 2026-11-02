package com.swiftdrop.tracking.api;

import com.swiftdrop.tracking.dto.TrackingResponse;
import com.swiftdrop.tracking.service.TrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tracking")
@RequiredArgsConstructor
public class TrackingController {

    private final TrackingService trackingService;

    /** Public (no login): see SecurityConfig. */
    @GetMapping("/{trackingNumber}")
    public ResponseEntity<TrackingResponse> track(@PathVariable String trackingNumber) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(trackingService.getByTrackingNumber(trackingNumber));
    }
}
