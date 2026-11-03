package com.swiftdrop.merchant.service;

import com.swiftdrop.auth.domain.User;
import com.swiftdrop.auth.repository.UserRepository;
import com.swiftdrop.common.exception.ConflictException;
import com.swiftdrop.merchant.domain.Merchant;
import com.swiftdrop.merchant.domain.MerchantStatus;
import com.swiftdrop.merchant.dto.MerchantMapper;
import com.swiftdrop.merchant.dto.MerchantResponseDto;
import com.swiftdrop.merchant.dto.RegisterMerchantRequest;
import com.swiftdrop.merchant.repository.MerchantRepository;
import com.swiftdrop.merchant.security.CurrentMerchantProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MerchantServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MerchantRepository merchantRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MerchantMapper merchantMapper;

    @Mock
    private CurrentMerchantProvider currentMerchantProvider;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private MerchantService merchantService;

    @Test
    @DisplayName("Successfully registers a merchant with MERCHANT user and PENDING status")
    void register_success() {
        RegisterMerchantRequest request = RegisterMerchantRequest.builder()
                .email(" Shop@Example.COM ")
                .password("secret123")
                .businessName("Apex Store")
                .registrationNumber("PV12345")
                .contactName("John Doe")
                .contactPhone("0712345678")
                .build();

        when(userRepository.findByEmail("shop@example.com")).thenReturn(Optional.empty());
        when(merchantRepository.existsByContactEmail("shop@example.com")).thenReturn(false);
        when(merchantRepository.existsByRegistrationNumber("PV12345")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed_secret123");

        User savedUser = new User();
        savedUser.setId(10L);
        savedUser.setEmail("shop@example.com");
        savedUser.setRole("MERCHANT");
        savedUser.setEnabled(true);
        savedUser.setProvider("LOCAL");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        Merchant savedMerchant = Merchant.builder()
                .id(20L)
                .userId(10L)
                .businessName("Apex Store")
                .registrationNumber("PV12345")
                .contactName("John Doe")
                .contactEmail("shop@example.com")
                .contactPhone("0712345678")
                .status(MerchantStatus.PENDING)
                .createdAt(Instant.now())
                .build();
        when(merchantRepository.save(any(Merchant.class))).thenReturn(savedMerchant);

        MerchantResponseDto expectedDto = MerchantResponseDto.builder()
                .id(20L)
                .userId(10L)
                .businessName("Apex Store")
                .registrationNumber("PV12345")
                .contactName("John Doe")
                .contactEmail("shop@example.com")
                .contactPhone("0712345678")
                .status(MerchantStatus.PENDING)
                .createdAt(savedMerchant.getCreatedAt())
                .build();
        when(merchantMapper.toDto(savedMerchant)).thenReturn(expectedDto);

        MerchantResponseDto result = merchantService.register(request);

        assertNotNull(result);
        assertEquals(20L, result.getId());
        assertEquals("shop@example.com", result.getContactEmail());
        assertEquals(MerchantStatus.PENDING, result.getStatus());

        // Verify User properties
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User capturedUser = userCaptor.getValue();
        assertEquals("shop@example.com", capturedUser.getEmail());
        assertEquals("hashed_secret123", capturedUser.getPasswordHash());
        assertEquals("MERCHANT", capturedUser.getRole());
        assertEquals("LOCAL", capturedUser.getProvider());
        assertTrue(capturedUser.isEnabled());

        // Verify Merchant properties
        ArgumentCaptor<Merchant> merchantCaptor = ArgumentCaptor.forClass(Merchant.class);
        verify(merchantRepository).save(merchantCaptor.capture());
        Merchant capturedMerchant = merchantCaptor.getValue();
        assertEquals(10L, capturedMerchant.getUserId());
        assertEquals("Apex Store", capturedMerchant.getBusinessName());
        assertEquals("PV12345", capturedMerchant.getRegistrationNumber());
        assertEquals(MerchantStatus.PENDING, capturedMerchant.getStatus());
    }

    @Test
    @DisplayName("Throws ConflictException when email is already in UserRepository")
    void register_duplicateEmailInUserRepository_throwsConflictException() {
        RegisterMerchantRequest request = RegisterMerchantRequest.builder()
                .email("existing@example.com")
                .password("secret123")
                .businessName("Apex Store")
                .registrationNumber("PV12345")
                .contactName("John Doe")
                .contactPhone("0712345678")
                .build();

        User existing = new User();
        existing.setEmail("existing@example.com");
        when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(existing));

        ConflictException ex = assertThrows(ConflictException.class, () -> merchantService.register(request));
        assertEquals("Email already in use", ex.getMessage());

        verify(userRepository, never()).save(any());
        verify(merchantRepository, never()).save(any());
    }

    @Test
    @DisplayName("Throws ConflictException when contact email exists in MerchantRepository")
    void register_duplicateEmailInMerchantRepository_throwsConflictException() {
        RegisterMerchantRequest request = RegisterMerchantRequest.builder()
                .email("existing@example.com")
                .password("secret123")
                .businessName("Apex Store")
                .registrationNumber("PV12345")
                .contactName("John Doe")
                .contactPhone("0712345678")
                .build();

        when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.empty());
        when(merchantRepository.existsByContactEmail("existing@example.com")).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, () -> merchantService.register(request));
        assertEquals("Email already in use", ex.getMessage());

        verify(userRepository, never()).save(any());
        verify(merchantRepository, never()).save(any());
    }

    @Test
    @DisplayName("Throws ConflictException when registration number already exists")
    void register_duplicateRegistrationNumber_throwsConflictException() {
        RegisterMerchantRequest request = RegisterMerchantRequest.builder()
                .email("new@example.com")
                .password("secret123")
                .businessName("Apex Store")
                .registrationNumber("PV12345")
                .contactName("John Doe")
                .contactPhone("0712345678")
                .build();

        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(merchantRepository.existsByContactEmail("new@example.com")).thenReturn(false);
        when(merchantRepository.existsByRegistrationNumber("PV12345")).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, () -> merchantService.register(request));
        assertEquals("Business registration number already registered", ex.getMessage());

        verify(userRepository, never()).save(any());
        verify(merchantRepository, never()).save(any());
    }

    @Test
    @DisplayName("Resolves logged-in merchant profile via CurrentMerchantProvider")
    void getMyProfile_success() {
        Merchant merchant = Merchant.builder()
                .id(55L)
                .userId(12L)
                .businessName("My Shop")
                .status(MerchantStatus.PENDING)
                .build();
        MerchantResponseDto dto = MerchantResponseDto.builder()
                .id(55L)
                .userId(12L)
                .businessName("My Shop")
                .status(MerchantStatus.PENDING)
                .build();

        when(currentMerchantProvider.getCurrentMerchant(authentication)).thenReturn(merchant);
        when(merchantMapper.toDto(merchant)).thenReturn(dto);

        MerchantResponseDto result = merchantService.getMyProfile(authentication);

        assertNotNull(result);
        assertEquals(55L, result.getId());
        assertEquals("My Shop", result.getBusinessName());
    }
}
