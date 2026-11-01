package com.swiftdrop.auth.api;

import com.swiftdrop.auth.domain.User;
import com.swiftdrop.auth.domain.UserAddress;
import com.swiftdrop.auth.dto.AddressDto;
import com.swiftdrop.auth.repository.UserAddressRepository;
import com.swiftdrop.auth.repository.UserRepository;
import com.swiftdrop.common.exception.ForbiddenException;
import com.swiftdrop.common.exception.NotFoundException;
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddressControllerTest {

    @Mock
    private UserAddressRepository addressRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AddressController addressController;

    private User userA;
    private User userB;
    private UserAddress addressOfUserA;
    private UserAddress addressOfUserB;

    @BeforeEach
    void setUp() {
        userA = new User();
        userA.setId(1L);
        userA.setEmail("usera@example.com");
        userA.setFullName("User A");
        userA.setRole("CUSTOMER");

        userB = new User();
        userB.setId(2L);
        userB.setEmail("userb@example.com");
        userB.setFullName("User B");
        userB.setRole("CUSTOMER");

        addressOfUserA = new UserAddress();
        addressOfUserA.setId(101L);
        addressOfUserA.setUser(userA);
        addressOfUserA.setAddressLabel("Home");
        addressOfUserA.setStreetAddress("123 Main St");
        addressOfUserA.setCity("Colombo");
        addressOfUserA.setDistrict("Colombo");

        addressOfUserB = new UserAddress();
        addressOfUserB.setId(202L);
        addressOfUserB.setUser(userB);
        addressOfUserB.setAddressLabel("Office");
        addressOfUserB.setStreetAddress("456 Galle Rd");
        addressOfUserB.setCity("Kandy");
        addressOfUserB.setDistrict("Kandy");

        // Set authenticated user to userA
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userA.getEmail(), null, Collections.emptyList())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("IDOR Defense: User A should NOT be able to delete User B's address (throws ForbiddenException)")
    void deleteAddress_whenNotOwner_throwsForbiddenException() {
        when(userRepository.findByEmail(userA.getEmail())).thenReturn(Optional.of(userA));
        when(addressRepository.findById(addressOfUserB.getId())).thenReturn(Optional.of(addressOfUserB));

        ForbiddenException exception = assertThrows(ForbiddenException.class, () ->
                addressController.deleteAddress(addressOfUserB.getId())
        );

        assertEquals("Unauthorized to delete this address", exception.getMessage());
        verify(addressRepository, never()).delete(any());
    }

    @Test
    @DisplayName("User A can successfully delete their own address")
    void deleteAddress_whenOwner_deletesSuccessfully() {
        when(userRepository.findByEmail(userA.getEmail())).thenReturn(Optional.of(userA));
        when(addressRepository.findById(addressOfUserA.getId())).thenReturn(Optional.of(addressOfUserA));

        ResponseEntity<Void> response = addressController.deleteAddress(addressOfUserA.getId());

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(addressRepository, times(1)).delete(addressOfUserA);
    }

    @Test
    @DisplayName("Throws NotFoundException when address does not exist")
    void deleteAddress_whenNotFound_throwsNotFoundException() {
        when(userRepository.findByEmail(userA.getEmail())).thenReturn(Optional.of(userA));
        when(addressRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                addressController.deleteAddress(999L)
        );
    }

    @Test
    @DisplayName("Retrieves only addresses for authenticated user")
    void getMyAddresses_returnsUserAddresses() {
        when(userRepository.findByEmail(userA.getEmail())).thenReturn(Optional.of(userA));
        when(addressRepository.findByUserId(userA.getId())).thenReturn(List.of(addressOfUserA));

        ResponseEntity<List<AddressDto>> response = addressController.getMyAddresses();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("Home", response.getBody().get(0).getAddressLabel());
    }

    @Test
    @DisplayName("Adds a new address for authenticated user")
    void addAddress_savesAndReturnsAddress() {
        when(userRepository.findByEmail(userA.getEmail())).thenReturn(Optional.of(userA));
        when(addressRepository.save(any(UserAddress.class))).thenAnswer(invocation -> {
            UserAddress saved = invocation.getArgument(0);
            saved.setId(102L);
            return saved;
        });

        AddressDto dto = AddressDto.builder()
                .addressLabel("Work")
                .streetAddress("789 Duplication Rd")
                .city("Colombo")
                .district("Colombo")
                .postalCode("00300")
                .isDefault(true)
                .build();

        ResponseEntity<AddressDto> response = addressController.addAddress(dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Work", response.getBody().getAddressLabel());
        assertTrue(response.getBody().isDefault());
    }
}
