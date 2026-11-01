package com.swiftdrop.auth.api;

import com.swiftdrop.auth.domain.User;
import com.swiftdrop.auth.repository.UserRepository;
import com.swiftdrop.auth.service.ReceiptService;
import com.swiftdrop.common.exception.ForbiddenException;
import com.swiftdrop.common.exception.NotFoundException;
import com.swiftdrop.parcel.domain.Address;
import com.swiftdrop.parcel.domain.Parcel;
import com.swiftdrop.parcel.repository.ParcelRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReceiptControllerTest {

    @Mock
    private ReceiptService receiptService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ParcelRepository parcelRepository;

    @InjectMocks
    private ReceiptController receiptController;

    private User customerA;
    private User customerB;
    private User admin;
    private Parcel parcelOfCustomerA;

    @BeforeEach
    void setUp() {
        customerA = new User();
        customerA.setId(10L);
        customerA.setEmail("customera@example.com");
        customerA.setFullName("Customer A");
        customerA.setRole("CUSTOMER");

        customerB = new User();
        customerB.setId(20L);
        customerB.setEmail("customerb@example.com");
        customerB.setFullName("Customer B");
        customerB.setRole("CUSTOMER");

        admin = new User();
        admin.setId(99L);
        admin.setEmail("admin@swiftdrop.lk");
        admin.setFullName("Admin User");
        admin.setRole("ADMIN");

        Address dropoff = new Address();
        dropoff.setCity("Kandy");
        dropoff.setDistrict("Kandy");
        dropoff.setLine1("123 Hill St");

        parcelOfCustomerA = new Parcel();
        parcelOfCustomerA.setId(1L);
        parcelOfCustomerA.setTrackingNumber("SD12345678AA");
        parcelOfCustomerA.setCustomerId(customerA.getId());
        parcelOfCustomerA.setReceiverName("Receiver Person");
        parcelOfCustomerA.setDropoff(dropoff);
        parcelOfCustomerA.setWeightKg(new BigDecimal("2.5"));
        parcelOfCustomerA.setFee(new BigDecimal("750.00"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("IDOR Defense: Customer B cannot download receipt for Customer A's parcel (throws ForbiddenException)")
    void downloadReceipt_whenNotOwnerAndNotAdmin_throwsForbiddenException() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(customerB.getEmail(), null, Collections.emptyList())
        );

        when(userRepository.findByEmail(customerB.getEmail())).thenReturn(Optional.of(customerB));
        when(parcelRepository.findByTrackingNumber(parcelOfCustomerA.getTrackingNumber()))
                .thenReturn(Optional.of(parcelOfCustomerA));

        ForbiddenException ex = assertThrows(ForbiddenException.class, () ->
                receiptController.downloadReceipt(parcelOfCustomerA.getTrackingNumber())
        );

        assertTrue(ex.getMessage().contains("IDOR Violation"));
        verify(receiptService, never()).generateDeliveryReceipt(any(), any(), any(), any(), anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("Parcel owner Customer A can download their receipt")
    void downloadReceipt_whenOwner_returnsPdf() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(customerA.getEmail(), null, Collections.emptyList())
        );

        byte[] fakePdf = new byte[]{1, 2, 3};

        when(userRepository.findByEmail(customerA.getEmail())).thenReturn(Optional.of(customerA));
        when(parcelRepository.findByTrackingNumber(parcelOfCustomerA.getTrackingNumber()))
                .thenReturn(Optional.of(parcelOfCustomerA));
        when(receiptService.generateDeliveryReceipt(
                eq(parcelOfCustomerA.getTrackingNumber()),
                eq(customerA.getFullName()),
                eq("Receiver Person"),
                eq("Kandy"),
                eq(2.5),
                eq(750.00)
        )).thenReturn(fakePdf);

        ResponseEntity<byte[]> response = receiptController.downloadReceipt(parcelOfCustomerA.getTrackingNumber());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertArrayEquals(fakePdf, response.getBody());
    }

    @Test
    @DisplayName("Admin can download receipt for any parcel")
    void downloadReceipt_whenAdmin_returnsPdf() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin.getEmail(), null, Collections.emptyList())
        );

        byte[] fakePdf = new byte[]{4, 5, 6};

        when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        when(parcelRepository.findByTrackingNumber(parcelOfCustomerA.getTrackingNumber()))
                .thenReturn(Optional.of(parcelOfCustomerA));
        when(receiptService.generateDeliveryReceipt(any(), any(), any(), any(), anyDouble(), anyDouble()))
                .thenReturn(fakePdf);

        ResponseEntity<byte[]> response = receiptController.downloadReceipt(parcelOfCustomerA.getTrackingNumber());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertArrayEquals(fakePdf, response.getBody());
    }

    @Test
    @DisplayName("Throws NotFoundException when parcel does not exist")
    void downloadReceipt_whenParcelNotFound_throwsNotFoundException() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(customerA.getEmail(), null, Collections.emptyList())
        );

        when(userRepository.findByEmail(customerA.getEmail())).thenReturn(Optional.of(customerA));
        when(parcelRepository.findByTrackingNumber("NON_EXISTENT")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                receiptController.downloadReceipt("NON_EXISTENT")
        );
    }
}
