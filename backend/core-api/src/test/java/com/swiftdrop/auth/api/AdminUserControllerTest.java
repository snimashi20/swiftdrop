package com.swiftdrop.auth.api;

import com.swiftdrop.auth.domain.User;
import com.swiftdrop.auth.dto.CreateStaffRequestDto;
import com.swiftdrop.auth.dto.StatusToggleDto;
import com.swiftdrop.auth.dto.UserResponseDto;
import com.swiftdrop.auth.repository.UserRepository;
import com.swiftdrop.common.exception.BadRequestException;
import com.swiftdrop.common.exception.ConflictException;
import com.swiftdrop.common.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

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

    @InjectMocks
    private AdminUserController adminUserController;

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
}
