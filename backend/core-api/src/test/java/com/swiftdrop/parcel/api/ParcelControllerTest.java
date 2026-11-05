package com.swiftdrop.parcel.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.swiftdrop.auth.domain.User;
import com.swiftdrop.auth.repository.UserRepository;
import com.swiftdrop.common.exception.NotFoundException;
import com.swiftdrop.parcel.dto.CreateParcelRequest;
import com.swiftdrop.parcel.dto.ParcelResponse;
import com.swiftdrop.parcel.service.ParcelService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

class ParcelControllerTest {

    private ParcelService parcelService;
    private UserRepository userRepository;
    private Authentication authentication;
    private ParcelController controller;

    @BeforeEach
    void setUp() {
        parcelService = mock(ParcelService.class);
        userRepository = mock(UserRepository.class);
        authentication = mock(Authentication.class);
        controller = new ParcelController(parcelService, userRepository);
        when(authentication.getName()).thenReturn("customer@example.com");
    }

    @Test
    void create_bookParcelForTheLoggedInCustomer() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(7L);
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(user));

        CreateParcelRequest request = new CreateParcelRequest();
        ParcelResponse parcelResponse = ParcelResponse.builder()
                .id(10L).trackingNumber("SDABCDEFGHIJ").build();
        when(parcelService.createForCustomer(7L, request)).thenReturn(parcelResponse);

        ResponseEntity<ParcelResponse> result = controller.create(request, authentication);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals("SDABCDEFGHIJ", result.getBody().getTrackingNumber());
    }

    @Test
    void create_unknownUser_bookingNeverStarts() {
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> controller.create(new CreateParcelRequest(), authentication));

        verify(parcelService, never()).createForCustomer(any(), any());
    }
}