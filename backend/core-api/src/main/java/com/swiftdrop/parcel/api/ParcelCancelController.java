package com.swiftdrop.parcel.api;

import com.swiftdrop.auth.domain.User;
import com.swiftdrop.auth.repository.UserRepository;
import com.swiftdrop.common.exception.NotFoundException;
import com.swiftdrop.parcel.dto.ParcelDetailResponse;
import com.swiftdrop.parcel.service.ParcelCancelService;
import com.swiftdrop.parcel.service.ParcelService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/parcels")
@RequiredArgsConstructor
public class ParcelCancelController {

    private final ParcelCancelService parcelCancelService;
    private final ParcelService parcelService;
    private final UserRepository userRepository;

    /** Cancel my parcel. Only allowed while it is still CREATED. */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ParcelDetailResponse cancel(@PathVariable Long id, Authentication authentication) {
        Long customerId = currentUserId(authentication);
        parcelCancelService.cancelForCustomer(customerId, id);
        return parcelService.getForCustomer(customerId, id);
    }

    private Long currentUserId(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new NotFoundException("User not found"));
        return user.getId();
    }
}