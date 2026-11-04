package com.swiftdrop.auth.api;

import com.swiftdrop.auth.domain.User;
import com.swiftdrop.auth.dto.ReceiptDataDto;
import com.swiftdrop.auth.repository.UserRepository;
import com.swiftdrop.auth.service.ReceiptService;
import com.swiftdrop.common.exception.ForbiddenException;
import com.swiftdrop.common.exception.NotFoundException;
import com.swiftdrop.parcel.domain.Address;
import com.swiftdrop.parcel.domain.Parcel;
import com.swiftdrop.parcel.domain.ParcelSize;
import com.swiftdrop.parcel.domain.ParcelStatus;
import com.swiftdrop.parcel.repository.ParcelRepository;
import com.swiftdrop.tracking.domain.StatusHistory;
import com.swiftdrop.tracking.repository.StatusHistoryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
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

    @Mock
    private StatusHistoryRepository statusHistoryRepository;

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
        customerA.setPhoneNumber("0771234567");
        customerA.setRole("CUSTOMER");

        customerB = new User();
        customerB.setId(20L);
        customerB.setEmail("customerb@example.com");
        customerB.setFullName("Customer B");
        customerB.setPhoneNumber("0719876543");
        customerB.setRole("CUSTOMER");

        admin = new User();
        admin.setId(99L);
        admin.setEmail("admin@swiftdrop.lk");
        admin.setFullName("Admin User");
        admin.setRole("ADMIN");

        Address pickup = new Address();
        pickup.setCity("Colombo");
        pickup.setDistrict("Colombo");
        pickup.setLine1("10 Galle Rd");

        Address dropoff = new Address();
        dropoff.setCity("Kandy");
        dropoff.setDistrict("Kandy");
        dropoff.setLine1("123 Hill St");

        parcelOfCustomerA = new Parcel();
        parcelOfCustomerA.setId(1L);
        parcelOfCustomerA.setTrackingNumber("SD12345678AA");
        parcelOfCustomerA.setCustomerId(customerA.getId());
        parcelOfCustomerA.setPickup(pickup);
        parcelOfCustomerA.setDropoff(dropoff);
        parcelOfCustomerA.setReceiverName("Receiver Person");
        parcelOfCustomerA.setReceiverPhone("0751112233");
        parcelOfCustomerA.setSize(ParcelSize.M);
        parcelOfCustomerA.setStatus(ParcelStatus.DELIVERED);
        parcelOfCustomerA.setWeightKg(new BigDecimal("2.50"));
        parcelOfCustomerA.setFee(new BigDecimal("750.00"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/v1/parcels/{id}/receipt: Owner downloads receipt by numeric ID")
    void getParcelReceipt_byNumericId_whenOwner_returnsPdf() {
        authenticateAs(customerA);

        byte[] fakePdf = new byte[]{0x25, 0x50, 0x44, 0x46}; // %PDF

        when(userRepository.findByEmail(customerA.getEmail())).thenReturn(Optional.of(customerA));
        when(parcelRepository.findById(1L)).thenReturn(Optional.of(parcelOfCustomerA));
        when(statusHistoryRepository.findByParcelIdOrderByChangedAtAsc(1L)).thenReturn(List.of(
                StatusHistory.builder()
                        .parcelId(1L)
                        .newStatus(ParcelStatus.CREATED)
                        .changedAt(Instant.now())
                        .changedByRole("CUSTOMER")
                        .build()
        ));
        when(receiptService.generateDeliveryReceipt(any(ReceiptDataDto.class))).thenReturn(fakePdf);

        ResponseEntity<byte[]> response = receiptController.getParcelReceipt("1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_PDF, response.getHeaders().getContentType());
        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).contains("receipt-SD12345678AA.pdf"));
        assertArrayEquals(fakePdf, response.getBody());
    }

    @Test
    @DisplayName("GET /api/v1/parcels/{id}/receipt: Owner downloads receipt by tracking number string")
    void getParcelReceipt_byTrackingNumber_whenOwner_returnsPdf() {
        authenticateAs(customerA);

        byte[] fakePdf = new byte[]{0x25, 0x50, 0x44, 0x46};

        when(userRepository.findByEmail(customerA.getEmail())).thenReturn(Optional.of(customerA));
        when(parcelRepository.findByTrackingNumber("SD12345678AA")).thenReturn(Optional.of(parcelOfCustomerA));
        when(statusHistoryRepository.findByParcelIdOrderByChangedAtAsc(1L)).thenReturn(Collections.emptyList());
        when(receiptService.generateDeliveryReceipt(any(ReceiptDataDto.class))).thenReturn(fakePdf);

        ResponseEntity<byte[]> response = receiptController.getParcelReceipt("SD12345678AA");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertArrayEquals(fakePdf, response.getBody());
    }

    @Test
    @DisplayName("IDOR Defense: Customer B cannot download Customer A's receipt by ID (throws ForbiddenException)")
    void getParcelReceipt_whenNotOwnerAndNotAdmin_throwsForbiddenException() {
        authenticateAs(customerB);

        when(userRepository.findByEmail(customerB.getEmail())).thenReturn(Optional.of(customerB));
        when(parcelRepository.findById(1L)).thenReturn(Optional.of(parcelOfCustomerA));

        ForbiddenException ex = assertThrows(ForbiddenException.class, () ->
                receiptController.getParcelReceipt("1")
        );

        assertTrue(ex.getMessage().contains("IDOR Violation"));
        verify(receiptService, never()).generateDeliveryReceipt(any(ReceiptDataDto.class));
    }

    @Test
    @DisplayName("Admin user can download any customer's receipt by parcel ID")
    void getParcelReceipt_whenAdmin_returnsPdf() {
        authenticateAs(admin);

        byte[] fakePdf = new byte[]{0x25, 0x50, 0x44, 0x46};

        when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        when(parcelRepository.findById(1L)).thenReturn(Optional.of(parcelOfCustomerA));
        when(userRepository.findById(customerA.getId())).thenReturn(Optional.of(customerA));
        when(statusHistoryRepository.findByParcelIdOrderByChangedAtAsc(1L)).thenReturn(Collections.emptyList());
        when(receiptService.generateDeliveryReceipt(any(ReceiptDataDto.class))).thenReturn(fakePdf);

        ResponseEntity<byte[]> response = receiptController.getParcelReceipt("1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertArrayEquals(fakePdf, response.getBody());
    }

    @Test
    @DisplayName("Throws NotFoundException when parcel does not exist")
    void getParcelReceipt_whenParcelNotFound_throwsNotFoundException() {
        authenticateAs(customerA);

        when(userRepository.findByEmail(customerA.getEmail())).thenReturn(Optional.of(customerA));
        when(parcelRepository.findById(999L)).thenReturn(Optional.empty());
        when(parcelRepository.findByTrackingNumber("999")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                receiptController.getParcelReceipt("999")
        );
    }

    @Test
    @DisplayName("Legacy route GET /api/v1/receipts/{trackingNumber} functions identically")
    void downloadReceipt_legacyRoute_worksForOwner() {
        authenticateAs(customerA);

        byte[] fakePdf = new byte[]{1, 2, 3};

        when(userRepository.findByEmail(customerA.getEmail())).thenReturn(Optional.of(customerA));
        when(parcelRepository.findByTrackingNumber("SD12345678AA")).thenReturn(Optional.of(parcelOfCustomerA));
        when(receiptService.generateDeliveryReceipt(any(ReceiptDataDto.class))).thenReturn(fakePdf);

        ResponseEntity<byte[]> response = receiptController.downloadReceipt("SD12345678AA");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertArrayEquals(fakePdf, response.getBody());
    }

    private void authenticateAs(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getEmail(), null, Collections.emptyList())
        );
    }
}
