package com.swiftdrop.courier;

import com.swiftdrop.auth.domain.User;
import com.swiftdrop.auth.repository.UserRepository;
import com.swiftdrop.common.exception.ForbiddenException;
import com.swiftdrop.courier.api.CourierController;
import com.swiftdrop.courier.service.CourierService;
import com.swiftdrop.dispatch.repository.CourierProfileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CourierControllerAuthorizationTest {

    @Mock
    private CourierService courierService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CourierProfileRepository courierProfileRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CourierController courierController;

    @Test
    @DisplayName("User without a courier profile gets 403 Forbidden")
    void userWithoutCourierProfileThrowsForbiddenException() {
        when(authentication.getName()).thenReturn("nocourier@swiftdrop.lk");

        User user = new User();
        user.setId(77L);
        user.setEmail("nocourier@swiftdrop.lk");

        when(userRepository.findByEmail("nocourier@swiftdrop.lk")).thenReturn(Optional.of(user));
        when(courierProfileRepository.findByUserId(77L)).thenReturn(Optional.empty());

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> courierController.getTodayDeliveries(authentication)
        );

        assertEquals("Not allowed: Courier profile not found for user", exception.getMessage());
    }
}