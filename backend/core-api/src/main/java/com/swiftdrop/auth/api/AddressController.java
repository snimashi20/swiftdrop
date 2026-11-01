package com.swiftdrop.auth.api;

import com.swiftdrop.auth.domain.User;
import com.swiftdrop.auth.domain.UserAddress;
import com.swiftdrop.auth.dto.AddressDto;
import com.swiftdrop.auth.repository.UserAddressRepository;
import com.swiftdrop.auth.repository.UserRepository;
import com.swiftdrop.common.exception.ForbiddenException;
import com.swiftdrop.common.exception.NotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/users/addresses")
public class AddressController {

    private final UserAddressRepository addressRepository;
    private final UserRepository userRepository;

    public AddressController(UserAddressRepository addressRepository, UserRepository userRepository) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<AddressDto>> getMyAddresses() {
        User user = getAuthenticatedUser();
        List<AddressDto> addresses = addressRepository.findByUserId(user.getId())
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(addresses);
    }

    @PostMapping
    public ResponseEntity<AddressDto> addAddress(@Valid @RequestBody AddressDto dto) {
        User user = getAuthenticatedUser();

        UserAddress address = new UserAddress();
        address.setUser(user);
        address.setAddressLabel(dto.getAddressLabel());
        address.setStreetAddress(dto.getStreetAddress());
        address.setCity(dto.getCity());
        address.setDistrict(dto.getDistrict());
        address.setPostalCode(dto.getPostalCode());
        address.setDefault(dto.isDefault());

        UserAddress saved = addressRepository.save(address);
        return ResponseEntity.ok(mapToDto(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAddress(@PathVariable Long id) {
        User user = getAuthenticatedUser();
        UserAddress address = addressRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Address not found with id: " + id));

        // Security Check: Unauthorized address deletion throws ForbiddenException (403)
        if (!address.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Unauthorized to delete this address");
        }

        addressRepository.delete(address);
        return ResponseEntity.noContent().build();
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found with email: " + email));
    }

    private AddressDto mapToDto(UserAddress address) {
        return AddressDto.builder()
                .id(address.getId())
                .addressLabel(address.getAddressLabel())
                .streetAddress(address.getStreetAddress())
                .city(address.getCity())
                .district(address.getDistrict())
                .postalCode(address.getPostalCode())
                .isDefault(address.isDefault())
                .build();
    }
}