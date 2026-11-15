package com.swiftdrop.auth.api;

import com.swiftdrop.auth.domain.User;
import com.swiftdrop.auth.dto.AdminUserDetailsResponseDto;
import com.swiftdrop.auth.dto.CreateStaffRequestDto;
import com.swiftdrop.auth.dto.RejectUserRequestDto;
import com.swiftdrop.auth.dto.StatusToggleDto;
import com.swiftdrop.auth.dto.UpdateRoleDto;
import com.swiftdrop.auth.dto.UserResponseDto;
import com.swiftdrop.auth.repository.UserRepository;
import com.swiftdrop.common.exception.BadRequestException;
import com.swiftdrop.common.exception.ConflictException;
import com.swiftdrop.common.exception.NotFoundException;
import com.swiftdrop.dispatch.domain.CourierProfile;
import com.swiftdrop.dispatch.domain.VehicleType;
import com.swiftdrop.dispatch.repository.CourierProfileRepository;
import com.swiftdrop.merchant.domain.Merchant;
import com.swiftdrop.merchant.domain.MerchantStatus;
import com.swiftdrop.merchant.repository.MerchantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MerchantRepository merchantRepository;

    @Mock
    private CourierProfileRepository courierProfileRepository;

    private AdminUserController adminUserController;

    @BeforeEach
    void setUp() {
        adminUserController = new AdminUserController(userRepository, passwordEncoder, merchantRepository, courierProfileRepository);
    }

    @Test
    @DisplayName("Admin gets all users without role filter")
    void getAllUsers_withoutFilter_returnsAllUsers() {
        User u1 = new User();
        u1.setId(1L);
        u1.setEmail("u1@example.com");
        u1.setRole("CUSTOMER");

        User u2 = new User();
        u2.setId(2L);
        u2.setEmail("u2@example.com");
        u2.setRole("COURIER");

        when(userRepository.findAll()).thenReturn(List.of(u1, u2));

        ResponseEntity<List<UserResponseDto>> response = adminUserController.getAllUsers(null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        verify(userRepository, times(1)).findAll();
        verify(userRepository, never()).findByRoleIgnoreCase(any());
    }

    @Test
    @DisplayName("Admin gets users filtered by role COURIER")
    void getAllUsers_withRoleFilter_returnsFilteredUsers() {
        User u1 = new User();
        u1.setId(2L);
        u1.setEmail("courier@example.com");
        u1.setRole("COURIER");

        when(userRepository.findByRoleIgnoreCase("COURIER")).thenReturn(List.of(u1));

        ResponseEntity<List<UserResponseDto>> response = adminUserController.getAllUsers("COURIER", null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("COURIER", response.getBody().get(0).getRole());
        verify(userRepository, times(1)).findByRoleIgnoreCase("COURIER");
        verify(userRepository, never()).findAll();
    }

    @Test
    @DisplayName("Admin updates user role successfully")
    void updateUserRole_success() {
        User user = new User();
        user.setId(5L);
        user.setEmail("user@example.com");
        user.setRole("CUSTOMER");

        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        UpdateRoleDto roleDto = UpdateRoleDto.builder().role("COURIER").build();
        ResponseEntity<UserResponseDto> response = adminUserController.updateUserRole(5L, roleDto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("COURIER", response.getBody().getRole());
        assertEquals("COURIER", user.getRole());
    }

    @Test
    @DisplayName("Updating role for non-existent user throws NotFoundException")
    void updateUserRole_whenNotFound_throwsNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        UpdateRoleDto roleDto = UpdateRoleDto.builder().role("ADMIN").build();
        assertThrows(NotFoundException.class, () ->
                adminUserController.updateUserRole(99L, roleDto)
        );
    }

    @Test
    @DisplayName("Admin creates staff account with valid role COURIER")
    void createStaffAccount_withValidCourierRole_success() {
        CreateStaffRequestDto request = new CreateStaffRequestDto();
        request.setEmail("courier@swiftdrop.lk");
        request.setFullName("Kamal Courier");
        request.setPhoneNumber("+94771234567");
        request.setPassword("securePassword123");
        request.setRole("COURIER");

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(10L);
            return u;
        });

        ResponseEntity<UserResponseDto> response = adminUserController.createStaffAccount(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("COURIER", response.getBody().getRole());
        assertEquals("courier@swiftdrop.lk", response.getBody().getEmail());
        assertTrue(response.getBody().isEnabled());
    }

    @Test
    @DisplayName("Admin creates staff account with valid role DISPATCHER")
    void createStaffAccount_withValidDispatcherRole_success() {
        CreateStaffRequestDto request = new CreateStaffRequestDto();
        request.setEmail("dispatcher@swiftdrop.lk");
        request.setFullName("Sunil Dispatcher");
        request.setPhoneNumber("+94779876543");
        request.setPassword("securePassword123");
        request.setRole("dispatcher"); // test case insensitivity

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(11L);
            return u;
        });

        ResponseEntity<UserResponseDto> response = adminUserController.createStaffAccount(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("DISPATCHER", response.getBody().getRole());
    }

    @Test
    @DisplayName("Staff role restriction: Creating staff with role other than COURIER or DISPATCHER throws BadRequestException")
    void createStaffAccount_withInvalidRole_throwsBadRequestException() {
        CreateStaffRequestDto request = new CreateStaffRequestDto();
        request.setEmail("fakeadmin@swiftdrop.lk");
        request.setFullName("Fake Admin");
        request.setPassword("securePassword123");
        request.setRole("ADMIN"); // Not allowed via staff creation endpoint

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                adminUserController.createStaffAccount(request)
        );

        assertEquals("Staff role must be either COURIER or DISPATCHER", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Creating staff account with existing email throws ConflictException")
    void createStaffAccount_withDuplicateEmail_throwsConflictException() {
        CreateStaffRequestDto request = new CreateStaffRequestDto();
        request.setEmail("existing@swiftdrop.lk");
        request.setFullName("Existing User");
        request.setPassword("securePassword123");
        request.setRole("COURIER");

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(new User()));

        assertThrows(ConflictException.class, () ->
                adminUserController.createStaffAccount(request)
        );
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Admin toggles user status from enabled to disabled")
    void toggleUserStatus_updatesStatusSuccessfully() {
        User user = new User();
        user.setId(5L);
        user.setEmail("user@example.com");
        user.setEnabled(true);

        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        StatusToggleDto toggle = new StatusToggleDto();
        toggle.setEnabled(false);

        ResponseEntity<UserResponseDto> response = adminUserController.toggleUserStatus(5L, toggle);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isEnabled());
    }

    @Test
    @DisplayName("Toggling status for non-existent user throws NotFoundException")
    void toggleUserStatus_whenNotFound_throwsNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        StatusToggleDto toggle = new StatusToggleDto();
        toggle.setEnabled(false);

        assertThrows(NotFoundException.class, () ->
                adminUserController.toggleUserStatus(99L, toggle)
        );
    }

    @Test
    @DisplayName("Admin approves a pending user account")
    void approveUser_activatesAccount() {
        User user = new User();
        user.setId(10L);
        user.setEmail("pending@example.com");
        user.setRole("COURIER");
        user.setStatus("PENDING_APPROVAL");
        user.setEnabled(false);

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        ResponseEntity<UserResponseDto> response = adminUserController.approveUser(10L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEnabled());
        assertEquals("ACTIVE", response.getBody().getStatus());
    }

    @Test
    @DisplayName("Admin rejects a pending user account")
    void rejectUser_deactivatesAccount() {
        User user = new User();
        user.setId(11L);
        user.setEmail("rejected@example.com");
        user.setRole("MERCHANT");
        user.setStatus("PENDING_APPROVAL");
        user.setEnabled(false);

        when(userRepository.findById(11L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        ResponseEntity<UserResponseDto> response = adminUserController.rejectUser(11L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isEnabled());
        assertEquals("REJECTED", response.getBody().getStatus());
    }

    @Test
    @DisplayName("Admin gets user details for a standard user")
    void getUserDetails_standardUser_returnsDetails() {
        User user = new User();
        user.setId(20L);
        user.setEmail("user20@example.com");
        user.setFullName("User Twenty");
        user.setPhoneNumber("0771234567");
        user.setRole("CUSTOMER");
        user.setProvider("LOCAL");
        user.setEnabled(true);
        user.setStatus("ACTIVE");

        when(userRepository.findById(20L)).thenReturn(Optional.of(user));

        ResponseEntity<AdminUserDetailsResponseDto> response = adminUserController.getUserDetails(20L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(20L, response.getBody().getId());
        assertEquals("user20@example.com", response.getBody().getEmail());
        assertNull(response.getBody().getMerchantProfile());
        assertNull(response.getBody().getCourierProfile());
    }

    @Test
    @DisplayName("Admin gets user details for a merchant with linked profile")
    void getUserDetails_merchantUser_returnsDetailsWithMerchantProfile() {
        User user = new User();
        user.setId(21L);
        user.setEmail("merchant@example.com");
        user.setFullName("Merchant User");
        user.setRole("MERCHANT");
        user.setStatus("PENDING_APPROVAL");

        Merchant merchant = Merchant.builder()
                .id(101L)
                .userId(21L)
                .businessName("Fresh Mart Ltd")
                .registrationNumber("PV123456")
                .contactName("Merchant User")
                .contactEmail("merchant@example.com")
                .contactPhone("0779876543")
                .status(MerchantStatus.PENDING)
                .build();

        when(userRepository.findById(21L)).thenReturn(Optional.of(user));
        when(merchantRepository.findByUserId(21L)).thenReturn(Optional.of(merchant));

        ResponseEntity<AdminUserDetailsResponseDto> response = adminUserController.getUserDetails(21L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getMerchantProfile());
        assertEquals("Fresh Mart Ltd", response.getBody().getMerchantProfile().getBusinessName());
        assertEquals("PV123456", response.getBody().getMerchantProfile().getRegistrationNumber());
    }

    @Test
    @DisplayName("Admin gets user details for a courier with linked profile")
    void getUserDetails_courierUser_returnsDetailsWithCourierProfile() {
        User user = new User();
        user.setId(22L);
        user.setEmail("courier@example.com");
        user.setFullName("Speedy Courier");
        user.setRole("COURIER");
        user.setStatus("PENDING_APPROVAL");

        CourierProfile courier = CourierProfile.builder()
                .id(202L)
                .userId(22L)
                .vehicleType(VehicleType.MOTORBIKE)
                .zoneId(5L)
                .active(true)
                .build();

        when(userRepository.findById(22L)).thenReturn(Optional.of(user));
        when(courierProfileRepository.findByUserId(22L)).thenReturn(Optional.of(courier));

        ResponseEntity<AdminUserDetailsResponseDto> response = adminUserController.getUserDetails(22L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCourierProfile());
        assertEquals("MOTORBIKE", response.getBody().getCourierProfile().getVehicleType());
        assertEquals(5L, response.getBody().getCourierProfile().getZoneId());
    }

    @Test
    @DisplayName("Admin gets user details throws 404 when user not found")
    void getUserDetails_notFound_throwsNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> adminUserController.getUserDetails(999L));
    }

    @Test
    @DisplayName("Admin rejects user with custom rejection reason")
    void rejectUser_withReason_setsRejectionReasonOnMerchant() {
        User user = new User();
        user.setId(23L);
        user.setEmail("pending-merchant@example.com");
        user.setRole("MERCHANT");
        user.setStatus("PENDING_APPROVAL");

        Merchant merchant = Merchant.builder()
                .id(102L)
                .userId(23L)
                .businessName("Bad Doc Store")
                .status(MerchantStatus.PENDING)
                .build();

        when(userRepository.findById(23L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(merchantRepository.findByUserId(23L)).thenReturn(Optional.of(merchant));

        RejectUserRequestDto rejectDto = new RejectUserRequestDto("BR document is blurry and unverified");
        ResponseEntity<UserResponseDto> response = adminUserController.rejectUser(23L, rejectDto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("REJECTED", response.getBody().getStatus());
        assertEquals(MerchantStatus.REJECTED, merchant.getStatus());
        assertEquals("BR document is blurry and unverified", merchant.getRejectionReason());
        verify(merchantRepository, times(1)).save(merchant);
    }
}
