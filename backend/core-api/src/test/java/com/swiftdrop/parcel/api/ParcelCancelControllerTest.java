package com.swiftdrop.parcel.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.swiftdrop.auth.domain.User;
import com.swiftdrop.auth.repository.UserRepository;
import com.swiftdrop.common.exception.NotFoundException;
import com.swiftdrop.parcel.domain.ParcelStatus;
import com.swiftdrop.parcel.dto.ParcelDetailResponse;
import com.swiftdrop.parcel.service.ParcelCancelService;
import com.swiftdrop.parcel.service.ParcelService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

class ParcelCancelControllerTest {

    private ParcelCancelService parcelCancelService;
    private ParcelService parcelService;
    private UserRepository userRepository;
    private Authentication authentication;
    private ParcelCancelController controller;

    @BeforeEach
    void setUp() {
        parcelCancelService = mock(ParcelCancelService.class);
        parcelService = mock(ParcelService.class);
        userRepository = mock(UserRepository.class);
        authentication = mock(Authentication.class);
        controller = new ParcelCancelController(parcelCancelService, parcelService, userRepository);
        when(authentication.getName()).thenReturn("customer@example.com");
    }

    @Test
    void cancel_cancelsMyParcelAndReturnsTheUpdatedParcel() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(7L);
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(user));
        ParcelDetailResponse updated = ParcelDetailResponse.builder()
                .id(1L).status(ParcelStatus.CANCELLED).build();
        when(parcelService.getForCustomer(7L, 1L)).thenReturn(updated);

        ParcelDetailResponse result = controller.cancel(1L, authentication);

        verify(parcelCancelService).cancelForCustomer(7L, 1L);
        assertEquals(ParcelStatus.CANCELLED, result.getStatus());
    }

    @Test
    void cancel_unknownUser_isNotFound() {
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> controller.cancel(1L, authentication));

        verify(parcelCancelService, never()).cancelForCustomer(7L, 1L);
    }
}